# ChitChat

A real-time cross-platform messaging application built with Spring Boot, JavaFX, and MySQL/PostgreSQL.

**Live App:** https://chitchat-dwhqarhneuf7grb8.canadacentral-01.azurewebsites.net

**Download Executable:** https://github.com/dmandaliya/ChitChat/releases/tag/v1.0.0

**Course:** COMP.2800 Software Development — University of Windsor, 2026W

---

## Features

- Public and private messaging (STOMP over WebSocket)
- Group chat rooms
- Friend requests and social graph
- User profiles with bio, status, and avatar
- Message reactions, typing indicators, read receipts
- GIF search integration (Tenor API)
- WebRTC voice/video calls (web frontend)
- Dark mode and theme preferences
- Desktop client (JavaFX) + Web client (HTML/CSS/JS) — same backend

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.4.3, Spring WebSocket (STOMP), Spring Data JPA |
| Database | MySQL 9.6 (local), PostgreSQL (Azure production) |
| Desktop Client | JavaFX 21, OkHttp3 |
| Web Frontend | HTML5, CSS3, Vanilla JS |
| Security | BCrypt password hashing |
| Deployment | Azure App Service + Azure Database for PostgreSQL |
| CI/CD | GitHub Actions |

---

## Quick Start (Local)

**Prerequisites:** Java 17+, Maven 3.8+, MySQL 8.0+

```bash
git clone https://github.com/dmandaliya/ChitChat.git
cd ChitChat

# Build all modules
mvn clean install -DskipTests

# Run the server
cd server
mvn spring-boot:run
```

Open http://localhost:8080 in your browser.

For detailed setup instructions see [docs/deployment_document.md](docs/deployment_document.md).

---

## Project Structure

```
chitchat/
├── shared/     — DTOs shared between server and client (Message, UserPreferences)
├── server/     — Spring Boot backend (REST API + WebSocket)
├── client/     — JavaFX desktop application
└── docs/       — User requirements, design, deployment, and user guide
```

---

## Database

**Schema:** [docs/schema.sql](docs/schema.sql)

The schema is auto-created by Hibernate on first run. To connect manually:

```
Host:     localhost
Port:     3306
Database: chitchat
Username: root
Password: (none)
```

**Production (Azure):** `chitchat-db.postgres.database.azure.com` — credentials managed via Azure App Service environment variables.

---

## Documentation

| Document | File |
|---|---|
| User Requirements | [docs/user_requirements.md](docs/user_requirements.md) |
| Design Document | [docs/design_document.md](docs/design_document.md) |
| Deployment Document | [docs/deployment_document.md](docs/deployment_document.md) |
| User Guide | [docs/user_guide.md](docs/user_guide.md) |
| Database Schema | [docs/schema.sql](docs/schema.sql) |

---

## Team

| Name | Role |
|---|---|
| Deep Mandaliya | Project Manager, Database |
| Ayden Sendrea | Backend / Server |
| Asma Mohamed | Web Frontend, Profile Page |
| Stephen-Caleb Fallon | Desktop Client (JavaFX) |
