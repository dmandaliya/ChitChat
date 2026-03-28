# Deployment Document
## ChitChat — Real-Time Messaging Application
**Course:** COMP.2800 Software Development
**Term:** 2026W | University of Windsor

---

## 1. Introduction

This document describes how to build, configure, and run the ChitChat application in both local development and production environments. It is intended for system administrators, course graders, and any developer who needs to stand up the system from source.

ChitChat consists of three deployable components:
- **Server** — Spring Boot 3.4.3 backend (REST + WebSocket)
- **Client** — JavaFX 21 desktop application
- **Web Frontend** — Static HTML/CSS/JS served by the server (no separate deployment)

---

## 2. Prerequisites / System Requirements

### 2.1 Required Software

| Software | Minimum Version | Notes |
|---|---|---|
| Java (OpenJDK) | 17 | Tested on Java 25; must be on PATH |
| Apache Maven | 3.8 | Must be on PATH |
| MySQL | 8.0+ (9.6 recommended) | Local dev only; not required for cloud |
| Git | Any recent | For cloning the repository |

### 2.2 Hardware Recommendations

| Resource | Minimum | Recommended |
|---|---|---|
| RAM | 2 GB | 4 GB |
| Disk | 500 MB | 2 GB |
| CPU | 1 core | 2+ cores |
| Network | Any | Stable broadband (for WebSocket) |

### 2.3 Port Requirements
- **8080** — Spring Boot server (must not be in use)
- **3306** — MySQL default port

---

## 3. Cloning the Repository

```bash
git clone https://github.com/<your-org>/ChitChat.git
cd ChitChat
```

The project root contains four directories: `shared/`, `server/`, `client/`, and `docs/`.

---

## 4. Database Setup (Local Development)

### 4.1 Install MySQL

**macOS (Homebrew):**
```bash
brew install mysql@9.6
brew services start mysql@9.6
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

**Windows:**
Download the MySQL Community Installer from dev.mysql.com and follow the GUI wizard.

### 4.2 Configure MySQL

ChitChat uses the `root` user with no password in local development. Verify MySQL is running and accessible:

```bash
mysql -u root -e "SELECT 1;"
```

If you have a root password, update the configuration file before running the server (see Section 5.1).

### 4.3 Database Creation

No manual schema creation is required. The Spring Boot server uses Hibernate with `ddl-auto=update`, which automatically creates the database and all tables on first launch, provided the database `chitchat` exists or the JDBC URL includes `createDatabaseIfNotExist=true` (it does by default).

The database will be created at:
```
jdbc:mysql://localhost:3306/chitchat
```

Tables created automatically:
- `users`, `user_friends`, `user_blocked`, `user_pending_requests`
- `chat_messages`, `message_reactions`
- `chat_rooms`, `room_members`

---

## 5. Building the Application

### 5.1 Configuration (if needed)

The server configuration file is at:
```
server/src/main/resources/application.properties
```

Default contents:
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/chitchat?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jackson.serialization.write-dates-as-timestamps=false
```

If your MySQL root account has a password, set it in `spring.datasource.password=`.

### 5.2 Build All Modules

From the project root, run:

```bash
mvn clean install -DskipTests
```

Expected output (summary):
```
[INFO] ChitChat Parent ........................... SUCCESS
[INFO] chitchat-shared ........................... SUCCESS
[INFO] chitchat-server ........................... SUCCESS
[INFO] chitchat-client ........................... SUCCESS
[INFO] BUILD SUCCESS
```

The build produces:
- `shared/target/shared-1.0-SNAPSHOT.jar`
- `server/target/server-1.0-SNAPSHOT.jar`
- `client/target/` (JavaFX runtime classes)

---

## 6. Running the Server

### 6.1 Method A: Maven Spring Boot Plugin (Development)

```bash
cd server
mvn spring-boot:run
```

### 6.2 Method B: JAR (Production / SCS VM)

```bash
java -jar server/target/server-1.0-SNAPSHOT.jar
```

### 6.3 Verifying the Server is Running

Once started, you should see:
```
Started ChitChatServerApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

Verify via browser or curl:
```bash
curl http://localhost:8080/
```

The web frontend (login page) should be returned.

### 6.4 Server Endpoints Summary

| Base Path | Description |
|---|---|
| `http://localhost:8080/` | Web frontend (login page) |
| `http://localhost:8080/chat.html` | Web chat interface |
| `http://localhost:8080/api/auth/` | Authentication REST API |
| `http://localhost:8080/api/users/` | User/friend/profile REST API |
| `http://localhost:8080/api/rooms/` | Room management REST API |
| `http://localhost:8080/api/messages/` | Message history REST API |
| `ws://localhost:8080/ws` | STOMP WebSocket endpoint |

---

## 7. Running the Desktop Client (JavaFX)

### 7.1 Default Configuration (Cloud Backend)

By default, the desktop client connects to the production DigitalOcean backend:
```
https://chitchat-pidj7.ondigitalocean.app
```

No additional configuration is required to use the cloud backend.

### 7.2 Local Backend Configuration

To point the client at a local server, set the environment variable before launching:

**macOS/Linux:**
```bash
export CHITCHAT_SERVER_URL=http://localhost:8080
```

**Windows (Command Prompt):**
```cmd
set CHITCHAT_SERVER_URL=http://localhost:8080
```

### 7.3 Running the Client

```bash
cd client
mvn javafx:run
```

Or, if you have the JAR and JavaFX runtime:
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar client/target/client-1.0-SNAPSHOT.jar
```

The login window (440×580 px) will appear. Register a new account or log in with an existing one.

---

## 8. Accessing the Web Frontend

The web interface is bundled with the server. Once the server is running, open a browser and navigate to:

```
http://localhost:8080/
```

Or for the production deployment:
```
https://chitchat-pidj7.ondigitalocean.app/
```

No installation is needed for the web client — it runs entirely in the browser.

---

## 9. SCS VM Deployment (For Graders)

Follow these steps to deploy and run ChitChat on the SCS virtual machine without any additional configuration.

### Step 1 — Install Prerequisites

```bash
# Check Java version (must be 17+)
java -version

# Check Maven
mvn -version

# Install MySQL if not present
sudo apt update && sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### Step 2 — Clone the Repository

```bash
git clone https://github.com/<org>/ChitChat.git
cd ChitChat
```

### Step 3 — Build

```bash
mvn clean install -DskipTests
```

### Step 4 — Start the Server

```bash
cd server
mvn spring-boot:run &
```

Wait until you see `Started ChitChatServerApplication`. The database and schema are created automatically.

### Step 5 — Access the Web Application

Open a browser on the VM and go to:
```
http://localhost:8080/
```

Register a new account and start chatting. No further setup is needed.

### Step 6 (Optional) — Run the Desktop Client

```bash
cd ../client
export CHITCHAT_SERVER_URL=http://localhost:8080
mvn javafx:run
```

---

## 10. Production Deployment (DigitalOcean)

The production backend is deployed to DigitalOcean App Platform:

| Setting | Value |
|---|---|
| Platform | DigitalOcean App Platform |
| Backend URL | https://chitchat-pidj7.ondigitalocean.app |
| Database | DigitalOcean managed PostgreSQL |
| Build command | `mvn clean install -DskipTests` |
| Run command | `java -jar server/target/server-1.0-SNAPSHOT.jar` |
| Port | 8080 (mapped to HTTPS by DigitalOcean) |

Spring Boot auto-detects PostgreSQL when the `DATABASE_URL` environment variable is set by the DigitalOcean platform.

---

## 11. Troubleshooting

| Problem | Likely Cause | Solution |
|---|---|---|
| `Port 8080 already in use` | Another process on 8080 | Run `lsof -i :8080` and kill the process, or change `server.port` in application.properties |
| `Access denied for user 'root'` | MySQL password set | Add password to `spring.datasource.password` in application.properties |
| `Communications link failure` | MySQL not running | Run `brew services start mysql` (macOS) or `sudo systemctl start mysql` (Linux) |
| `UnsupportedClassVersionError` | Java version too old | Ensure `java -version` shows 17 or higher |
| `JavaFX runtime components are missing` | JavaFX not on module path | Use `mvn javafx:run` from the client module rather than running the JAR directly |
| `BUILD FAILURE` in Maven | Missing dependency | Ensure you run `mvn clean install` from the project **root** first, not a sub-module |
| WebSocket not connecting | Firewall / proxy | Ensure port 8080 is open; check that no corporate proxy blocks WebSocket upgrades |
| Messages not appearing | Client connected to wrong server | Check `CHITCHAT_SERVER_URL` env var; ensure client and server point to the same host |
