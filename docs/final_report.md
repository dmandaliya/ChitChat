# ChitChat — Final Project Report
**Course:** COMP.2800 Software Development
**Professor:** Dr. Andreas S. Maniatis
**Term:** 2026W | University of Windsor
**Group:** 7

---

## Abstract

ChitChat is a full-featured, cross-platform real-time messaging application developed as the group project for COMP.2800 Software Development. The system provides instant messaging, group chat rooms, a social friend graph, voice/video call signalling, and a range of personalization features. It is built on a Spring Boot backend with a MySQL/PostgreSQL database, a JavaFX desktop client, and a plain HTML/CSS/JavaScript web frontend. Communication between client and server is handled over both REST HTTP and STOMP WebSocket, enabling low-latency real-time delivery of messages, typing indicators, and read receipts. The application is deployed on DigitalOcean App Platform and is accessible at https://chitchat-pidj7.ondigitalocean.app.

---

## 1. Project Scope — Problem Description

Modern communication is dominated by instant messaging platforms such as WhatsApp, Discord, and Slack. These platforms, while powerful, are closed ecosystems with no transparency into how they work at a technical level. The goal of ChitChat was to design and implement a functionally complete messaging application from scratch, demonstrating the full software development lifecycle: requirements gathering, architectural design, iterative implementation, testing, and deployment.

The core problem ChitChat solves is the need for a self-hosted, open-source messaging solution that supports:
- Real-time one-on-one and group communication
- A persistent social graph (friends, blocking)
- Rich media sharing (images, GIFs, voice notes)
- Voice and video calling via WebRTC
- Cross-platform access (desktop + web browser)

The project was scoped to deliver a working, deployable system within a single academic term, demonstrating practical software engineering skills including multi-tier architecture, database design, real-time protocol implementation, and cloud deployment.

---

## 2. Team Involvement and Project Organization

### 2.1 Team Members

| Name | Student ID | Role |
|---|---|---|
| Deep Mandaliya | 110184234 | Project Manager, Database |
| Ayden Sendrea | — | Backend / Server, Read Receipts |
| Asma Mohamed | — | Web Frontend, Profile Page |
| Stephen-Caleb Fallon | — | Desktop Client (JavaFX) |

### 2.2 Project Organization

The project was organized into three development sprints:

**Sprint 1 — Prototype**
Established the basic communication foundation using raw TCP sockets. This sprint validated the core concept and helped the team align on the message format and client-server model before introducing a full framework.

**Sprint 2 — Spring Boot + WebSocket**
Migrated from raw sockets to a Spring Boot backend with STOMP WebSocket support and MySQL persistence. Introduced the JavaFX desktop client and a web frontend. Core features implemented: authentication, public chat, private messaging, friend system, and group rooms.

**Sprint 3 — Feature Completion and Deployment**
Added read receipts, typing indicators, message reactions, GIF search, voice/video call signalling, user profiles and preferences, dark mode, desktop notifications, and deployed the backend to DigitalOcean App Platform with a managed PostgreSQL database.

### 2.3 Tools and Workflow
- **Version control:** Git / GitHub (`github.com/dmandaliya/ChitChat`)
- **Build system:** Apache Maven (multi-module)
- **IDE:** VS Code / IntelliJ IDEA
- **Project tracking:** Group chat coordination
- **Deployment:** DigitalOcean App Platform

---

## 3. Project Design and Development

### 3.1 Architecture Overview

ChitChat follows a three-tier client-server architecture:

- **Presentation Tier:** JavaFX 21 desktop client and a browser-based HTML/CSS/JS web frontend
- **Application Tier:** Spring Boot 3.4.3 server exposing REST endpoints and a STOMP WebSocket broker
- **Data Tier:** MySQL 9.6 for local development, PostgreSQL on DigitalOcean for production

### 3.2 Technology Stack

| Component | Technology | Version |
|---|---|---|
| Backend | Spring Boot | 3.4.3 |
| Database (local) | MySQL | 9.6 |
| Database (production) | PostgreSQL | Cloud-managed |
| Desktop Client | JavaFX | 21.0.2 |
| Build Tool | Apache Maven | 3.8+ |
| Java | OpenJDK | 17+ |
| Real-time Protocol | STOMP over WebSocket | SockJS fallback |
| REST Client (client) | OkHttp3 | 4.12.0 |
| JSON | Jackson | 2.17.2 |
| Password Hashing | BCrypt | Spring Security |
| GIF Search | Tenor API | v2 |

### 3.3 Module Structure

The project is organized as a Maven multi-module build:

- **shared** — Shared DTOs and enums (`Message`, `MessageType`, `UserPreferences`) used by both server and client
- **server** — Spring Boot application with JPA entities, repositories, services, and controllers
- **client** — JavaFX application with FXML views, controllers, and service classes

### 3.4 Key Implementation Decisions

**Single Message DTO for all event types**
Rather than defining separate classes for chat messages, typing events, read receipts, and call signals, we used a single `Message` class with a `MessageType` enum field. This simplified the WebSocket handler significantly and kept the wire format consistent.

**Manual STOMP implementation on the client**
The JavaFX client implements the STOMP handshake manually over `org.java_websocket` rather than using a full STOMP client library. This avoided module system conflicts with Java's JPMS and gave us full control over the reconnection logic, which uses exponential backoff up to a 30-second delay.

**BCrypt password hashing**
User passwords are hashed with BCrypt before storage. Plaintext passwords are never persisted or logged anywhere in the system. The `EncryptionService` wraps Spring Security Crypto's `BCryptPasswordEncoder`.

**Hibernate schema auto-creation**
The database schema is managed by Hibernate with `ddl-auto=update`. There are no manual SQL migration files — the schema is created automatically on first server startup from the JPA entity definitions.

**DigitalOcean deployment**
The backend is deployed on DigitalOcean App Platform with a managed PostgreSQL database. The desktop client defaults to the DigitalOcean URL and can be redirected to a local server via the `CHITCHAT_SERVER_URL` environment variable.

### 3.5 Database Schema

The following tables are automatically created by Hibernate:

| Table | Purpose |
|---|---|
| `users` | User accounts with BCrypt-hashed passwords and preferences (JSON) |
| `user_friends` | ManyToMany join table for the friend graph |
| `user_blocked` | ManyToMany join table for blocked relationships |
| `user_pending_requests` | ManyToMany join table for pending friend requests |
| `chat_messages` | Persisted messages (public, private, room) with UUID primary keys |
| `message_reactions` | Emoji reactions linked to chat messages |
| `chat_rooms` | Group chat room definitions |
| `room_members` | ManyToMany join table for room membership |

---

## 4. Findings and Future Work

### 4.1 Findings

**What worked well:**
- The STOMP WebSocket layer proved reliable for real-time delivery. Message latency was consistently under 200 ms on local network and under 500 ms on the DigitalOcean deployment.
- Spring Boot's auto-configuration significantly reduced boilerplate. Hibernate's `ddl-auto=update` made schema management seamless across development and deployment environments.
- The shared Maven module pattern worked well for keeping the `Message` DTO in sync between client and server without duplication.
- The JavaFX FXML + CSS approach allowed clean separation of UI layout and styling, and the CSS class-based theming (dark mode, bubble colors, font sizes) was straightforward to implement.

**Challenges encountered:**
- The Java Platform Module System (JPMS) added complexity to the JavaFX client build, requiring careful `module-info.java` configuration and `--add-opens` flags in some cases.
- The SCS VM was unavailable within the submission timeframe. The DigitalOcean deployment was used as an alternative, which the professor acknowledged as acceptable given the circumstances.
- WebRTC call signalling works fully in the browser. The desktop client (JavaFX) handles the signalling messages but does not render live video, as JavaFX has no native WebRTC support.
- Message deduplication required client-side handling (800 ms window) because the server's dual-send pattern for private messages (user queue + topic fallback) occasionally caused duplicates.

### 4.2 Future Work

Given more time, the following enhancements would be prioritized:

- **Desktop video calls:** Integrate a WebView or GStreamer to bring WebRTC video support to the JavaFX client
- **Message search:** Full-text search across message history
- **File sharing:** Support for arbitrary file attachments beyond images
- **Push notifications:** Mobile-style push notifications when the desktop app is in the background
- **End-to-end encryption:** Client-side encryption for private messages
- **Admin controls:** Room moderation tools (kick/ban members, message deletion)

---

## 5. References

1. Spring Boot Documentation — https://docs.spring.io/spring-boot/docs/3.4.3/reference/html/
2. JavaFX 21 Documentation — https://openjfx.io/javadoc/21/
3. STOMP Protocol Specification — https://stomp.github.io/stomp-specification-1.2.html
4. OkHttp3 Documentation — https://square.github.io/okhttp/
5. Jackson Documentation — https://github.com/FasterXML/jackson-docs
6. Tenor GIF API — https://developers.google.com/tenor/guides/quickstart
7. DigitalOcean App Platform — https://docs.digitalocean.com/products/app-platform/
8. BCrypt — Niels Provos and David Mazières, "A Future-Adaptable Password Scheme", USENIX 1999

---

## Appendices

The following documents are attached as appendices to this report:

- **Appendix A:** User Requirements and Analysis Document
- **Appendix B:** Design Document
- **Appendix C:** Deployment Document
- **Appendix D:** User Guide

*(See docs/ folder in the repository or attached files)*
