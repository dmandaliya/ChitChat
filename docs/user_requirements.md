# User Requirements and Analysis Document
## ChitChat — Real-Time Messaging Application
**Course:** COMP.2800 Software Development
**Term:** 2026W | University of Windsor

---

## 1. Introduction

### 1.1 Purpose
This document defines the user requirements and functional/non-functional specifications for ChitChat, a real-time messaging application developed as part of the COMP.2800 group project. It serves as the authoritative reference for what the system must do and how it must behave from both the user's and the system's perspective.

### 1.2 Project Scope
ChitChat is a cross-platform communication platform consisting of:
- A **JavaFX 21** desktop client application
- A **Spring Boot 3.4.3** backend server with RESTful and WebSocket APIs
- A **web-based frontend** (HTML/CSS/JavaScript) served directly from the backend
- A **MySQL** relational database for persistence

The system supports real-time public and private messaging, group chat rooms, a social friend graph, media sharing, voice/video call signalling, and customizable user preferences.

### 1.3 Background and Motivation
Modern communication relies heavily on instant messaging platforms. ChitChat addresses the need for a self-hosted, feature-rich messaging solution that can be deployed both locally and on cloud infrastructure. Unlike commercial alternatives, ChitChat is purpose-built for course demonstration and academic evaluation, while exhibiting production-grade software engineering practices.

---

## 2. Stakeholders and User Profiles

### 2.1 Stakeholders

| Stakeholder | Role | Interest |
|---|---|---|
| End Users | Primary users of the chat system | Send/receive messages, manage contacts |
| Group Admins | Users who create/manage chat rooms | Room creation and membership management |
| System Administrators | Operate backend/database | Deployment, uptime, database management |
| Course Graders (Dr. Maniatis) | Academic evaluators | Running and assessing the application |
| Development Team | COMP.2800 student group | Building and maintaining the system |

### 2.2 User Profiles

**Regular User**
A person who registers for ChitChat to communicate with friends in real time. They expect an intuitive interface, fast message delivery, and reliable connectivity. They may use either the desktop or web client depending on their device.

**Group Room Admin**
A user who creates a group chat room for a specific purpose (e.g., a study group or team channel). They manage membership and are responsible for the room's content.

**System Administrator**
A technically proficient person responsible for running the server and database. They interact with deployment scripts, configuration files, and monitoring tools rather than the chat interface itself.

---

## 3. Functional Requirements

### 3.1 Authentication and Account Management

| ID | Requirement | Priority |
|---|---|---|
| FR-001 | The system shall allow a new user to register with first name, last name, username, and password. | High |
| FR-002 | The system shall enforce password complexity: minimum 8 characters, at least one uppercase letter, and at least one special symbol. | High |
| FR-003 | The system shall store passwords using BCrypt hashing; plaintext passwords must never be persisted. | High |
| FR-004 | The system shall allow a registered user to log in with their username and password. | High |
| FR-005 | The system shall allow a logged-in user to log out, updating their last-seen timestamp. | High |
| FR-006 | The system shall enforce unique usernames across all registered accounts. | High |

### 3.2 Messaging

| ID | Requirement | Priority |
|---|---|---|
| FR-007 | The system shall support a public chat channel visible to all connected users. | High |
| FR-008 | The system shall support private direct messages between two users. | High |
| FR-009 | The system shall support group chat rooms where multiple users can exchange messages. | High |
| FR-010 | The system shall persist the last 50 messages for each channel (public, private, room) and deliver them as history upon connection. | High |
| FR-011 | The system shall deliver a read receipt (✓✓) to the sender when the recipient views a private message. | Medium |
| FR-012 | The system shall broadcast a typing indicator to the recipient while a user is composing a message. | Medium |
| FR-013 | The system shall allow users to react to messages with emoji reactions. | Medium |
| FR-014 | The system shall support sending image files and voice recordings as message attachments. | Medium |
| FR-015 | The system shall integrate with the Tenor GIF API to allow users to search and send GIFs. | Low |

### 3.3 Social Features

| ID | Requirement | Priority |
|---|---|---|
| FR-016 | The system shall allow users to search for other users by username or display name. | High |
| FR-017 | The system shall allow users to send, accept, and reject friend requests. | High |
| FR-018 | The system shall allow users to remove existing friends from their friend list. | Medium |
| FR-019 | The system shall allow users to block other users, preventing blocked users from sending them messages. | High |
| FR-020 | The system shall allow users to unblock previously blocked users. | Medium |
| FR-021 | The system shall display online/offline presence status for friends in the sidebar. | Medium |

### 3.4 Group Rooms

| ID | Requirement | Priority |
|---|---|---|
| FR-022 | The system shall allow any authenticated user to create a new group chat room with a name and optional description. | High |
| FR-023 | The system shall allow users to join existing group rooms. | High |
| FR-024 | The system shall allow users to leave group rooms they have previously joined. | Medium |
| FR-025 | The system shall display a list of all available rooms to authenticated users. | Medium |

### 3.5 Voice and Video Calls

| ID | Requirement | Priority |
|---|---|---|
| FR-026 | The system shall support WebRTC call signalling (offer, answer, ICE candidates, end, reject) via the STOMP WebSocket layer. | Medium |
| FR-027 | The web client shall support live voice and video calls using the browser's WebRTC API. | Medium |
| FR-028 | The desktop client shall initiate call signalling and display incoming call notifications. | Low |

### 3.6 User Profile and Preferences

| ID | Requirement | Priority |
|---|---|---|
| FR-029 | The system shall allow users to set a personal bio and select an availability status (Online, Away, Busy, Offline). | Medium |
| FR-030 | The system shall allow users to toggle dark mode for the desktop interface. | Low |
| FR-031 | The system shall allow users to choose a chat bubble color theme. | Low |
| FR-032 | The system shall allow users to configure notification preferences (enable/disable, read receipts). | Low |
| FR-033 | The system shall allow users to control privacy settings (online status visibility, last seen). | Low |
| FR-034 | The system shall allow users to adjust the font size (Small, Medium, Large). | Low |

### 3.7 Notifications

| ID | Requirement | Priority |
|---|---|---|
| FR-035 | The desktop client shall display system-level desktop notifications for incoming messages when the app is not in focus. | Medium |

---

## 4. Non-Functional Requirements

| ID | Requirement | Category |
|---|---|---|
| NFR-001 | Messages must be delivered to recipients in under 500 milliseconds under normal network conditions. | Performance |
| NFR-002 | The system must support at least 50 concurrent WebSocket connections without degradation. | Scalability |
| NFR-003 | The desktop client must run on Windows, macOS, and Linux with Java 17+ and JavaFX 21. | Portability |
| NFR-004 | The web client must function in any modern browser (Chrome, Firefox, Safari, Edge) without plugins. | Compatibility |
| NFR-005 | The WebSocket client must automatically reconnect with exponential backoff upon network disconnection. | Reliability |
| NFR-006 | All passwords must be hashed with BCrypt before storage; no plaintext credentials may appear in logs or responses. | Security |
| NFR-007 | The REST API must return appropriate HTTP status codes (200, 400, 401, 404, 500) for all endpoints. | Correctness |
| NFR-008 | The system must auto-create the database schema on first launch (Hibernate ddl-auto=update). | Operability |
| NFR-009 | The application must be buildable from source using a single Maven command: `mvn clean install`. | Maintainability |
| NFR-010 | The system must deduplicate WebSocket messages client-side within an 800 ms window to prevent duplicate display. | Correctness |

---

## 5. Use Case Descriptions

### UC-001: Register a New Account

| Field | Description |
|---|---|
| **Actor** | Unregistered User |
| **Preconditions** | The user has launched the application and is on the Login screen |
| **Main Flow** | 1. User selects the "Register" tab. 2. User enters first name, last name, username, and password. 3. User clicks the Register button. 4. System validates: username unique, password meets complexity rules. 5. System hashes the password with BCrypt and stores the account. 6. System auto-logs in the user and opens the Chat screen. |
| **Alternate Flow** | 4a. Username already exists → system displays "Username taken" error. 4b. Password fails complexity → system displays specific error message. |
| **Postconditions** | A new user account is created; user is authenticated and in the main chat view. |

---

### UC-002: Send a Private Message

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | User is logged in and has at least one friend in their friend list |
| **Main Flow** | 1. User clicks on a friend's name in the sidebar. 2. System loads private message history (last 50). 3. User types a message and clicks Send. 4. Client sends message via STOMP `/app/chat.sendPrivate`. 5. Server delivers message to the recipient's `/user/queue/private` topic. 6. Recipient sees message appear in real time. |
| **Alternate Flow** | 5a. Recipient is offline → message is stored in DB; delivered when recipient reconnects and loads history. |
| **Postconditions** | Message is persisted in the database and displayed for both parties. |

---

### UC-003: Create and Join a Group Room

| Field | Description |
|---|---|
| **Actor** | Authenticated User |
| **Preconditions** | User is logged in |
| **Main Flow** | 1. User clicks "+ New" in the Group Rooms section of the sidebar. 2. User enters a room name and optional description. 3. System creates the room and auto-adds the creator as a member. 4. Room appears in the user's sidebar. 5. Other users can search and join the room via the room list. |
| **Alternate Flow** | 2a. Room name is blank → system displays validation error. |
| **Postconditions** | Room exists in the database; creator is a member; other users can join. |

---

### UC-004: Send and Accept a Friend Request

| Field | Description |
|---|---|
| **Actor** | Authenticated User (Sender), Authenticated User (Receiver) |
| **Preconditions** | Both users are registered; neither has blocked the other |
| **Main Flow** | 1. Sender searches for the receiver by username. 2. Sender clicks "Add Friend". 3. System adds sender to receiver's pending requests list. 4. Receiver opens Friend Requests panel and sees pending request. 5. Receiver clicks "Accept". 6. System adds both users to each other's friend lists. |
| **Alternate Flow** | 5a. Receiver clicks "Reject" → sender is removed from pending; no friendship is created. |
| **Postconditions** | Both users appear in each other's friends list and can exchange private messages. |

---

### UC-005: Initiate a Voice/Video Call

| Field | Description |
|---|---|
| **Actor** | Authenticated User (Caller), Authenticated User (Callee) |
| **Preconditions** | Both users are online and connected via WebSocket |
| **Main Flow** | 1. Caller opens a private chat with callee and clicks the call button. 2. Client sends a STOMP `call.offer` message containing a WebRTC SDP offer. 3. Callee receives an incoming call notification. 4. Callee accepts → client sends `call.answer` with SDP answer. 5. Both sides exchange ICE candidates via `call.ice`. 6. WebRTC peer connection is established; media streams. |
| **Alternate Flow** | 4a. Callee rejects → client sends `call.reject`; caller sees "Call rejected" notification. |
| **Postconditions** | Peer-to-peer audio/video session is active (web client) or call state is tracked (desktop). |

---

## 6. Constraints and Assumptions

### 6.1 Constraints
- The system is built for Java 17+; it will not compile or run on Java 8 or Java 11.
- The desktop client requires JavaFX 21 to be available either bundled or on the module path.
- Local development requires MySQL 9.6 installed with an accessible `root` account.
- Production deployment targets DigitalOcean App Platform with a managed PostgreSQL database.
- GIF search functionality requires an active internet connection and a valid Tenor API key.

### 6.2 Assumptions
- All users have a stable internet connection sufficient for WebSocket communication.
- The server machine has port 8080 available and not blocked by a firewall.
- Users understand basic messaging application conventions (friends, chat rooms, emoji reactions).
- The SCS VM will have Java 17+ and Maven 3.8+ available for grader evaluation.

---

## 7. Summary

ChitChat is a full-featured, real-time messaging platform that satisfies 35 functional requirements across authentication, messaging, social graph management, group rooms, call signalling, and personalization. The system is designed for reliability, security, and cross-platform compatibility. This document provides the authoritative specification against which the delivered software should be evaluated, and serves as the foundation for the accompanying Design Document, Deployment Document, and User Guide.
