# Software Design Document
## ChitChat — Real-Time Messaging Application
**Course:** COMP.2800 Software Development
**Term:** 2026W | University of Windsor

---

## 1. Introduction

### 1.1 Purpose
This document describes the architectural and detailed software design of the ChitChat application. It provides an authoritative reference for the system's structure, component responsibilities, data model, communication protocols, and design decisions.

### 1.2 Scope
The design covers all three deployable components of the ChitChat system:
- The Spring Boot backend server
- The JavaFX desktop client
- The HTML/CSS/JavaScript web frontend

---

## 2. System Architecture Overview

ChitChat follows a **three-tier client-server architecture**:

```
┌─────────────────────┐       ┌──────────────────────────┐       ┌─────────────┐
│   Presentation Tier │       │      Application Tier     │       │  Data Tier  │
│                     │       │                           │       │             │
│  JavaFX Desktop App │◄─────►│   Spring Boot Server      │◄─────►│   MySQL /   │
│  Web Browser (HTML) │       │   (REST + WebSocket)      │       │ PostgreSQL  │
└─────────────────────┘       └──────────────────────────┘       └─────────────┘
```

**Communication channels:**
- **HTTP/REST** — Authentication, user management, message history, room management
- **STOMP over WebSocket** — Real-time message delivery, typing indicators, read receipts, call signalling

The server is the single source of truth. Clients never communicate directly with each other; all messages are routed through the server's STOMP broker.

---

## 3. Module Structure

The project is a Maven multi-module build with three sub-modules:

| Module | Artifact | Purpose |
|---|---|---|
| `shared` | JAR | Shared DTOs and enums used by both server and client |
| `server` | JAR (Spring Boot) | Backend application server |
| `client` | JavaFX application | Desktop chat client |

The `shared` module declares a Java Platform Module System (JPMS) `module-info.java`, exporting `com.chitchat.shared` to both the server and client modules.

---

## 4. Component Design

### 4.1 Shared Module

**`Message.java`** — Primary transport DTO (implements `Serializable`)
- Fields: `id` (UUID), `type` (MessageType), `sender`, `receiver`, `content`, `roomId`, `timestamp`
- Includes a no-arg constructor required by Jackson for deserialization

**`MessageType.java`** — Enum defining all message categories:
- Auth: `LOGIN`, `LOGIN_SUCCESS`, `LOGIN_FAIL`
- Chat: `PUBLIC_MESSAGE`, `PRIVATE_MESSAGE`, `ROOM_MESSAGE`
- Social: `USER_LIST`, `LOGOUT`, `READ_RECEIPT`, `TYPING`
- Calls: `CALL_OFFER`, `CALL_ANSWER`, `CALL_ICE`, `CALL_END`, `CALL_REJECT`
- System: `ERROR`

**`UserPreferences.java`** — User settings DTO (implements `Serializable`)
- Display: `darkMode`, `fontSize` (1–3), `bubbleColour`, `fontStyle`
- Notifications: `notis`, `showReadReceipts`
- Privacy: `onlineStatus`, `lastSeen`
- Profile: `status`, `bio`

---

### 4.2 Server Module

#### 4.2.1 Configuration Layer

**`WebSocketConfig`** — Configures the STOMP message broker:
- Enables broker on `/topic` (broadcast) and `/user` (targeted) prefixes
- Application destination prefix: `/app`
- STOMP endpoint: `/ws` with SockJS fallback for browser compatibility
- Custom `ChannelInterceptor` reads the `login` header from STOMP CONNECT frames and sets the WebSocket `Principal`, enabling per-user message routing

#### 4.2.2 Controller Layer

**`AuthController`** (`/api/auth`) — Stateless REST controller for authentication:
- `POST /register` — Validates input, delegates to `UserService.register()`
- `POST /login` — Delegates to `UserService.login()`, returns user DTO
- `POST /logout` — Updates last-seen timestamp

**`ChatController`** (STOMP `@MessageMapping`) — Real-time message handler:
- Maintains `onlineUsers: Map<String, String>` (username → sessionId)
- Handles: join, leave, public send, private send, room send, read receipt, typing, WebRTC signals
- Listens to `SessionDisconnectEvent` for graceful cleanup

**`UserController`** (`/api/users`) — Social graph and profile management:
- Friends: list, add, remove
- Friend requests: list, send, accept, reject
- Blocked: list, block, unblock
- Profile: get, update
- Preferences: get, update
- Search: `GET /search?q=` — case-insensitive LIKE query

**`ChatRoomController`** (`/api/rooms`) — Room lifecycle management:
- Create, list all, join, leave

**`MessageHistoryController`** (`/api/messages`) — Historical message retrieval:
- Public history (last 50), private history (bidirectional), room history (last 50)
- Add emoji reaction: `POST /{id}/react`

#### 4.2.3 Service Layer

**`UserService`** — Business logic for user accounts:
- `register()`: validates uniqueness + password complexity, hashes password via BCrypt
- `login()`: BCrypt verification, sets `loggedIn = true`, records `lastOnline`
- `logout()`: sets `loggedIn = false`, records `lastOnline`
- `searchUsers()`: delegates to JPQL LIKE query

**`ChatMessageService`** — Message persistence:
- Converts `Message` DTO → `ChatMessage` entity and saves
- Retrieves ordered message history per channel type

**`FriendService`** — Social graph operations (all `@Transactional`):
- `sendFriendRequest()`: adds sender to receiver's `pendingRequests`
- `acceptFriendRequest()`: promotes from pending to `friendList` on both sides
- `rejectFriendRequest()`: removes from pending without creating friendship
- `removeFriend()`: removes from both parties' `friendList`

**`ChatRoomService`** — Room lifecycle (all `@Transactional`):
- `createRoom()`: persists room, auto-adds creator as member
- `joinRoom()` / `leaveRoom()`: adds/removes user from `members` collection

**`EncryptionService`** — Thin wrapper around Spring Security's `BCryptPasswordEncoder`

#### 4.2.4 Repository Layer

All repositories extend `JpaRepository`, with custom JPQL queries where needed:

| Repository | Custom Queries |
|---|---|
| `UserRepository` | `findByUsername`, `existsByUsername`, `searchByUsernameOrName` (LIKE) |
| `ChatMessageRepository` | `findTop50ByType`, `findPrivateMessages` (bidirectional), `findTop50ByRoomId` |
| `ChatRoomRepository` | `findRoomsByUsername` (via join table) |
| `MessageReactionRepository` | Standard JPA only |

---

### 4.3 Client Module

#### 4.3.1 Application Entry Point

**`ChatClientApp`** — Extends `javafx.application.Application`:
- Resolves server URL via: `System.getProperty("server.url")` → `CHITCHAT_SERVER_URL` env var → hardcoded DigitalOcean default
- Loads `login.fxml` as the initial scene (440×580 px, non-resizable)

#### 4.3.2 Service Layer (Client)

**`WebSocketService`** — Manual STOMP implementation over `org.java_websocket`:
- Performs HTTP upgrade to WebSocket, then sends STOMP CONNECT frame with `login` header
- Subscribes to:
  - `/topic/public` — Public messages
  - `/user/queue/private` — Direct messages
  - `/topic/users` — Online user list
  - `/user/queue/receipts` — Read receipt confirmations
  - `/user/queue/call` — WebRTC signalling
  - `/topic/room/{roomId}` — Dynamic per-room subscriptions
- Auto-reconnects with exponential backoff (up to 30 s delay)
- Client-side deduplication: tracks recent message IDs in a rolling window (800 ms)
- Exposes callbacks: `onMessage`, `onUserList`

**`ApiService`** — OkHttp3-based REST client:
- Sends JSON payloads using Jackson `ObjectMapper` with `JavaTimeModule`
- Covers all REST endpoints: auth, users, friends, rooms, messages, reactions
- Returns deserialized response objects or raw JSON nodes

#### 4.3.3 Controller Layer (Client)

**`LoginController`** — Manages the login/register tab UI:
- Async operations via `CompletableFuture` to avoid blocking the JavaFX Application Thread
- UI updates dispatched back via `Platform.runLater()`

**`ChatController`** — Main application controller (largest class):
- Manages sidebar (friends, rooms) and main chat area
- Tracks state maps: `friendOnline`, `friendUnread`, `roomUnread`, `reactionsByMessageId`, `receiptStatusByMessageId`
- Connects `WebSocketService` and `ApiService` on login
- Message deduplication window: 800 ms
- Notification cooldown: 1500 ms (prevents notification flooding)
- Dispatches WebRTC signalling: call offer → answer → ICE → end/reject

**`ProfileController`** — Edits user bio and status:
- Displays avatar with user initials
- Persists changes via `ApiService.updateProfile()`

#### 4.3.4 Model Layer (Client)

**`UserSession`** — Singleton holding the current session state (username, fname, lname). Implemented with lazy initialization; shared across all controllers.

---

## 5. Database Design

### 5.1 Schema Overview

The schema is managed by Hibernate (`ddl-auto=update`) and auto-created on first server startup. No manual SQL migration is required.

### 5.2 Tables

**`users`**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| fname | VARCHAR | NOT NULL |
| lname | VARCHAR | NOT NULL |
| username | VARCHAR | NOT NULL, UNIQUE |
| hashed_password | VARCHAR | NOT NULL |
| last_online | VARCHAR | nullable |
| logged_in | BOOLEAN | default false |
| preferences | TEXT | JSON-encoded UserPreferences |

**`user_friends`** — Join table for bidirectional ManyToMany friend relationship
**`user_blocked`** — Join table for block relationships
**`user_pending_requests`** — Join table for pending friend requests

**`chat_messages`**
| Column | Type | Constraints |
|---|---|---|
| id | VARCHAR (UUID) | PK |
| type | VARCHAR (enum) | NOT NULL |
| sender | VARCHAR | nullable |
| receiver | VARCHAR | nullable |
| room_id | VARCHAR | nullable |
| content | TEXT | |
| timestamp | DATETIME | |

**`message_reactions`**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| message_id | VARCHAR | FK → chat_messages.id |
| username | VARCHAR | |
| emoji | VARCHAR | |

**`chat_rooms`**
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR | NOT NULL |
| description | VARCHAR | nullable |
| created_by | VARCHAR | NOT NULL |

**`room_members`** — Join table for room ManyToMany membership

### 5.3 Relationships

```
users ──< user_friends >── users       (ManyToMany, self-referential)
users ──< user_blocked >── users       (ManyToMany, self-referential)
users ──< user_pending_requests >── users
chat_messages ──< message_reactions    (OneToMany)
chat_rooms ──< room_members >── users  (ManyToMany)
```

---

## 6. Communication Protocol Design

### 6.1 REST API

All REST endpoints follow standard HTTP conventions:

| Verb | Meaning |
|---|---|
| GET | Read data |
| POST | Create resource or perform action |
| PUT | Update existing resource |
| DELETE | Remove resource |

Responses use JSON. Error responses include an appropriate HTTP status code (400, 401, 404, 500) and a descriptive message body.

### 6.2 STOMP over WebSocket

The real-time layer follows the STOMP 1.2 protocol over WebSocket:

**Connection handshake:**
```
Client → Server:  HTTP GET /ws/websocket  (WebSocket upgrade)
Client → Server:  STOMP CONNECT  (login: <username>)
Server → Client:  STOMP CONNECTED
```

**Message send flow (private message):**
```
Client → Server:  STOMP SEND /app/chat.sendPrivate  {Message JSON}
Server → Client:  STOMP MESSAGE /user/queue/private  {Message JSON}  (to receiver)
Server → Client:  STOMP MESSAGE /user/queue/private  {Message JSON}  (to sender, echo)
```

**Read receipt flow:**
```
Receiver → Server:  STOMP SEND /app/chat.read  {messageId, sender}
Server → Sender:    STOMP MESSAGE /user/queue/receipts  {READ_RECEIPT}
```

### 6.3 WebRTC Call Signalling

Call signalling is relayed entirely through the STOMP broker; no direct peer connection is attempted before signalling completes:

```
Caller  → /app/call.offer  → Server  → /user/queue/call  → Callee
Callee  → /app/call.answer → Server  → /user/queue/call  → Caller
Both    → /app/call.ice    → Server  → /user/queue/call  → Peer
Either  → /app/call.end    → Server  → /user/queue/call  → Peer
```

---

## 7. Security Design

| Concern | Solution |
|---|---|
| Password storage | BCrypt hashing via Spring Security Crypto (`EncryptionService`) |
| Transport security | HTTPS/WSS in production (DigitalOcean TLS termination) |
| WebSocket identity | STOMP `login` header mapped to Spring `Principal`; used for user-targeted delivery |
| Input validation | `UserService.register()` validates username uniqueness and password complexity before persistence |
| SQL injection | Prevented by Spring Data JPA parameterized queries and JPQL |
| No session tokens | Stateless design; client re-authenticates on reconnect; server tracks online state in memory |

---

## 8. UI Design Overview

### 8.1 Desktop Client (JavaFX)

The desktop UI is defined in FXML and styled with CSS:

**Login Screen** (`login.fxml`) — 440×580 px, non-resizable
- Warm gradient background (lemon → rose)
- Tabbed card: Login | Register
- Error label below form fields

**Chat Screen** (`chat.fxml`) — Horizontal split layout
- **Sidebar** (prefWidth=280, dark #1c1c1c background):
  - Brand header, action icon buttons (settings, blocked, profile, logout)
  - Friends list with online status indicators
  - Group rooms list
- **Main chat area** (light #e8f1f2 background):
  - Chat header with room/friend name and call buttons
  - Scrollable message area with styled message bubbles
  - Typing indicator label
  - Input bar (attach, GIF, mic, text field, send)

**Profile Screen** (`profile.fxml`) — Card overlay
- Circular avatar with user initials
- Status combo box, bio text area

**Theme support:** CSS class-based theming (`.pref-dark`, `.pref-bubble-blue/teal/orange/pink`, `.pref-font-sm/md/lg`)

### 8.2 Web Frontend (HTML/CSS/JS)

The web frontend mirrors the desktop feature set with browser-native capabilities:
- Icon rail (left), sidebar (friends/rooms), main chat area
- Incoming call overlay with Accept/Reject buttons
- Video call screen with `<video>` elements for local and remote streams
- Reaction picker, add-friend modal, settings panel
- SockJS + STOMP.js for WebSocket communication

---

## 9. Design Patterns Used

| Pattern | Where Applied |
|---|---|
| **MVC (Model-View-Controller)** | JavaFX: FXML (View) + Controller classes + Service/Model classes |
| **Singleton** | `UserSession` — ensures one session object per JVM |
| **Observer / Listener** | `WebSocketService` callbacks (`onMessage`, `onUserList`); JavaFX event listeners |
| **Repository Pattern** | Spring Data JPA repositories abstract all database access |
| **Service Layer** | `UserService`, `FriendService`, `ChatRoomService`, `ChatMessageService` encapsulate business logic |
| **DTO (Data Transfer Object)** | `Message` and `UserPreferences` in shared module; isolates transport model from entity |
| **Strategy (partial)** | Server URL resolution: system property → env var → default constant |
| **Decorator** | `UserPreferencesConverter` adapts `UserPreferences` ↔ JSON string for JPA persistence |

---

## 10. Summary

ChitChat's design is centered on a clean separation between transport, business logic, and persistence layers on the server, and a service/controller split on the client. The STOMP WebSocket layer enables low-latency real-time communication while the REST API handles stateful operations. Hibernate automates schema management, and BCrypt ensures secure credential handling throughout. The modular Maven structure and JPMS module declarations ensure clean dependency boundaries between shared, server, and client code.
