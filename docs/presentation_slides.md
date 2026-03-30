# ChitChat — D.5 Presentation Slides
## COMP.2800 | Group 7 | University of Windsor

---

## SLIDE 1 — Cover

**ChitChat**
*A Real-Time Messaging Application*

COMP.2800 Software Development
Professor: Dr. Andreas S. Maniatis
University of Windsor — Winter 2026
Group 7

Deep Mandaliya · Ayden Sendrea · Asma Mohamed · Stephen-Caleb Fallon

Submitted: March 29, 2026

---

## SLIDE 2 — Team Members and Roles

**Our Team**

| Name | Role | Responsibilities |
|---|---|---|
| Deep Mandaliya | Project Manager · Database Lead | Schema design, JPA entities, Maven config, Azure deployment, integration testing, documentation |
| Ayden Sendrea | Backend Lead | Spring Boot server, REST API, STOMP WebSocket broker, ChatController, read receipt logic |
| Asma Mohamed | Frontend Lead | Web frontend (HTML/CSS/JS), user profile page, settings UI, browser WebRTC call interface, user guide |
| Stephen-Caleb Fallon | Desktop Client Lead | JavaFX app, FXML layouts, manual STOMP client, dark mode, desktop notifications, CSS theming |

---

## SLIDE 3 — Problem Scope

**The Problem**

- Messaging platforms like Discord, WhatsApp, and Slack are closed ecosystems
- No visibility into the protocols, schemas, or architectures that make real-time messaging work
- Students need hands-on experience building real-time, multi-user systems from scratch
- No open, educational alternative that covers the full software development lifecycle

**Our Goal**

- Build a production-quality messaging app from first principles
- Cover every layer: database design, REST API, WebSocket, JavaFX desktop UI, web frontend, cloud deployment
- Support 10+ concurrent users with sub-second delivery, persistent data, and BCrypt-secured accounts
- Deploy live on the public internet throughout the course term

---

## SLIDE 4 — Overall Solution — Architecture

**Three-Tier Architecture**

- **Presentation Tier:** JavaFX 21 Desktop App + HTML/CSS/JS Web Client (dark mode, FXML/CSS theming)
- **Application Tier:** Spring Boot 3.4.3 — REST API + STOMP WebSocket broker + WebRTC signalling
- **Data Tier:** MySQL 9.6 (local dev) · PostgreSQL on Azure (production) · Hibernate ORM · BCrypt

Maven multi-module build: **shared** · **server** · **client**

---

## SLIDE 5 — Overall Solution — Key Features

**What ChitChat Can Do**

- **Real-Time Messaging** — Public, private & group chat with sub-second delivery via STOMP WebSocket
- **Social Graph** — Friend requests, accept, remove, and block workflows
- **Rich Media** — Emoji reactions, GIF search via Tenor API, image sharing
- **Read Receipts** — Delivery confirmation with live typing indicators
- **Cross-Platform** — JavaFX desktop + web browser sharing the same Spring Boot backend
- **Voice & Video Calls** — WebRTC signalling for peer-to-peer communication
- **User Profiles** — Status text, bio, avatar, display preferences
- **Dark Mode** — CSS theming with adjustable font sizes for accessibility
- **Cloud Deployed** — Live on Microsoft Azure App Service with managed PostgreSQL database

---

## SLIDE 6 — Live Demo

**Live Demo**

Live at: https://chitchat-dwhqarhneuf7grb8.canadacentral-01.azurewebsites.net

*Demo flow (open in browser — no installation needed):*

1. Register a new account
2. Send a public message
3. Add a friend and send a private DM
4. Create / join a group room
5. Show read receipts + typing indicator
6. Toggle dark mode

JavaFX desktop client + web browser sharing the same Spring Boot backend

---

## SLIDE 7 — Challenges & Lessons Learned

**Challenges**

- WebSocket reliability — STOMP reconnection and message ordering under network instability
- Cross-platform parity — keeping JavaFX desktop and web clients in sync with one backend
- Cloud deployment — migrating from MySQL to PostgreSQL on Azure App Service + GitHub Actions CI/CD
- WebRTC integration — NAT traversal and STUN/TURN server configuration for voice calls
- Maven multi-module — managing shared dependencies across server, client, and shared modules

**Lessons Learned**

- Start simple, iterate — MVP-first approach let us ship early and add features incrementally
- Interface-driven design — shared contracts between client and server prevented integration bugs
- Test in production early — deploying to Azure from week 1 caught environment-specific issues
- Clear role ownership — each member owned a layer, reducing merge conflicts and bottlenecks

---

## SLIDE 8 — References

**References**

1. Spring Boot 3.4.3 — docs.spring.io/spring-boot
2. JavaFX 21 — openjfx.io/javadoc/21
3. STOMP Protocol v1.2 — stomp.github.io
4. OkHttp3 — square.github.io/okhttp
5. Jackson Databind — github.com/FasterXML/jackson-docs
6. Tenor GIF API v2 — developers.google.com/tenor
7. Microsoft Azure App Service — learn.microsoft.com/azure/app-service
8. BCrypt — Provos & Mazières, USENIX 1999
9. WebRTC 1.0 Spec (W3C) — w3.org/TR/webrtc
10. Apache Maven — maven.apache.org/guides

---

## SLIDE 9 — Q&A

**Thank You!**

*Questions?*

ChitChat — Group 7
COMP.2800 · University of Windsor · Winter 2026

Live App: https://chitchat-dwhqarhneuf7grb8.canadacentral-01.azurewebsites.net
Source Code: github.com/dmandaliya/ChitChat

Deep Mandaliya · Ayden Sendrea · Asma Mohamed · Stephen-Caleb Fallon

---

## PRESENTATION NOTES (7 minutes)

**Min 0:00–0:30 — Cover + Intro (Slide 1)**
"Hi, we're Group 7 and we built ChitChat — a full-stack, cross-platform real-time messaging application built from scratch for COMP.2800."

**Min 0:30–1:00 — Team (Slide 2)**
Briefly introduce each member and their ownership area.

**Min 1:00–2:00 — Problem + Goal (Slide 3)**
"Platforms like Discord are black boxes. We wanted to build one ourselves to understand every layer — WebSocket protocols, database design, cloud deployment, and cross-platform UI."

**Min 2:00–2:30 — Architecture (Slide 4)**
"Three-tier architecture: two independent clients — a JavaFX desktop app and a web frontend — both backed by a single Spring Boot server over REST and STOMP WebSocket."

**Min 2:30–3:00 — Features (Slide 5)**
Quick walkthrough of the feature set — real-time messaging, social graph, WebRTC calls, dark mode, Azure deployment.

**Min 3:00–4:30 — Live Demo (Slide 6)**
Open https://chitchat-dwhqarhneuf7grb8.canadacentral-01.azurewebsites.net in browser.
Register → public message → DM a friend → group room → show read receipts → dark mode.

**Min 4:30–5:00 — Challenges + Wrap up (Slide 7)**
"Key challenge was cross-platform parity and cloud deployment. Lesson: deploy early and often."

**Min 5:00–7:00 — Q&A (Slides 8–9)**
References visible, then thank you slide. Take questions.
