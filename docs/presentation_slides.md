# ChitChat — D.5 Presentation Slides
## COMP.2800 | Group 7 | University of Windsor

---

## SLIDE 1 — Cover

**ChitChat**
*A Real-Time Messaging Application*

COMP.2800 Software Development
University of Windsor — Winter 2026
Group 7

---

## SLIDE 2 — Team Members and Roles

**Our Team**

| Name | Role |
|---|---|
| Deep Mandaliya | Project Manager, Database |
| Ayden Sendrea | Backend / Server |
| Asma Mohamed | Web Frontend, Profile Page |
| Stephen-Caleb Fallon | Desktop Client (JavaFX) |

---

## SLIDE 3 — Problem Scope

**The Problem**

- Existing messaging platforms (WhatsApp, Discord, Slack) are closed ecosystems
- No transparency into how they work technically
- Students need a hands-on project that covers the full software development lifecycle

**Our Goal**
Build a fully working, deployable messaging application from scratch — covering architecture, real-time protocols, database design, and cloud deployment.

---

## SLIDE 4 — Overall Solution

**What We Built**

ChitChat is a cross-platform real-time messaging app with:

- **Desktop App** — JavaFX 21 (Windows, macOS, Linux)
- **Web App** — HTML/CSS/JS in any browser
- **Backend** — Spring Boot 3.4.3 + MySQL/PostgreSQL
- **Real-time** — STOMP WebSocket for instant delivery

**Key Features:**
- Public chat, private DMs, group rooms
- Friend system (add, remove, block)
- Read receipts + typing indicators
- Message reactions + GIF search
- Voice/video call signalling (WebRTC)
- Dark mode + user preferences
- Deployed live on DigitalOcean

---

## SLIDE 5 — Architecture

**System Architecture**

```
JavaFX Desktop App  ──┐
                       ├──► Spring Boot Server ──► MySQL / PostgreSQL
Web Browser (HTML)  ──┘         ↕
                           STOMP WebSocket
                           REST API (HTTP)
```

**Tech Stack:**
Java 17 · Spring Boot 3.4.3 · JavaFX 21 · MySQL · OkHttp3 · Jackson · BCrypt · DigitalOcean

---

## SLIDE 6 — Live Demo

**Live Demo**

🌐 https://chitchat-pidj7.ondigitalocean.app

*Demo flow:*
1. Register a new account
2. Send a public message
3. Add a friend and send a private message
4. Create / join a group room
5. Show read receipts + typing indicator

---

## SLIDE 7 — References

**References**

1. Spring Boot — https://spring.io/projects/spring-boot
2. JavaFX — https://openjfx.io
3. STOMP Protocol — https://stomp.github.io
4. OkHttp3 — https://square.github.io/okhttp
5. Tenor GIF API — https://developers.google.com/tenor
6. DigitalOcean App Platform — https://digitalocean.com/products/app-platform
7. BCrypt — Provos & Mazières, USENIX 1999

---

## SLIDE 8 — Q&A

**Thank You**

*Questions?*

ChitChat — Group 7
COMP.2800 · University of Windsor · Winter 2026

🌐 https://chitchat-pidj7.ondigitalocean.app
📁 github.com/dmandaliya/ChitChat

---

## PRESENTATION NOTES (7 minutes)

**Min 0:00–0:30 — Cover + intro**
"Hi, we're Group 7 and we built ChitChat, a full-stack real-time messaging app."

**Min 0:30–1:30 — Problem + solution (Slides 3-4)**
"Existing platforms are black boxes. We wanted to build one ourselves to understand every layer — WebSocket protocols, database design, cloud deployment."

**Min 1:30–2:30 — Architecture (Slide 5)**
"The system has three tiers — a JavaFX desktop client and a web frontend both connecting to a Spring Boot server over REST and WebSocket. Messages are delivered in real time via STOMP."

**Min 2:30–4:30 — Live demo (Slide 6)**
Live demo of the app. Register → send message → DM a friend → show read receipts.

**Min 4:30–5:00 — Wrap up**
"We're deployed on DigitalOcean, the code is on GitHub, and we've got full documentation including user requirements, design doc, deployment guide, and user guide."

**Min 5:00–7:00 — Q&A**
