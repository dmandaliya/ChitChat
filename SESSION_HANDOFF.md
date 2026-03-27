# Session Handoff

## Current State
- Date: 2026-03-26
- Branch: desktop-app
- HEAD: 1adc52b
- Working tree: clean
- **STATUS**: Live QA Gate 1 in progress (public/private/room messaging delivery)

## What Was Completed In This Round

### Gate 1 QA Defects Fixed
1. **Desktop client server URL misconfiguration**
   - Issue: Hardcoded to Render backend instead of localhost
   - Fix: Implemented `resolveServerUrl()` with 3-tier resolution (property → env → localhost default)
   - File: ChatClientApp.java

2. **Shared module enum mismatch (MessageType.TYPING)**
   - Issue: Runtime deserialization error when receiving TYPING messages
   - Fix: Maven installed current shared module to local cache
   - Command: `mvn -pl shared -am install -DskipTests`

3. **FXML tooltip syntax errors**
   - Issue: Invalid `tooltip="string"` attribute on 4 sidebar buttons
   - Fix: Updated to proper nested `<tooltip><Tooltip text="..."/>` syntax
   - File: chat.fxml

4. **Friend list not updating after acceptance**
   - Issue: No periodic refresh, no auto-tracking of incoming private peers
   - Fix: Added 8-second roster refresh timeline + auto-track via `ensureFriendTracked()`
   - File: ChatController.java

5. **Private messages not visible until manual refresh**
   - Issue: Server only publishing to `/user/queue/private` (not subscribed by manual STOMP client)
   - Fix: Client now subscribes to `/topic/user.{username}` on CONNECTED; server publishes to both
   - Files: WebSocketService.java (client), ChatController.java (server)

6. **Blank alert popups on friend request sent**
   - Issue: `showInfo()` and `showError()` lacked title and null-safe fallback content
   - Fix: Added title + defensive null checks with fallback messages
   - File: ChatController.java

### Feature Addition: Remove Friend
- Implemented Edit Friends dialog consolidating send request + remove friend workflows
- Allows selective friend removal without account re-registration for rapid test iteration
- Clears local tracking maps (friendUnread, friendLastActivity, etc.) on removal
- Safely exits private chat if removed friend is currently selected
- Files: ChatController.java (method overhaul), chat.fxml (button label change)

### Build Validation
- All three modules (parent, shared, client, server) compile cleanly
- Last successful compile: `mvn -pl client -am compile` (0.956s, no errors)

## Recent Desktop Checkpoints
- 2026-03-26 QA Session: Fixed 6 critical defects blocking message delivery
  - Server URL resolution (localhost default + property/env overrides)
  - Shared module enum mismatch (TYPING)
  - FXML tooltip syntax (4 buttons in sidebar)
  - Friend list refresh (8-second timeline + auto-track)
  - Private message realtime delivery (topic-per-user subscriptions)
  - Alert UX hardening (title + null-safe fallback content)
  - Feature: Remove friend without account re-registration
- cdae69d feat(client): finalize desktop for reconnect, previews, and notifications

## Confirmed Build Baseline
- Maven compile succeeded for all modules in this QA session:
  - `mvn clean compile` (3.786s, all 4 modules)
  - `mvn -pl shared -am install` (shared-1.0-SNAPSHOT.jar deployed)
  - `mvn -pl client -am compile` (0.956s, client module with all fixes)

## Next Planned Work

### Gate 1 QA: Message Delivery (In Progress)
**Subtests** (all code fixes applied, awaiting live verification):
1. ✅ Public message delivery (both directions) — code fixed, needs client launch
2. ✅ Private message delivery (realtime, no manual refresh) — code fixed, needs client launch
3. ⏳ Room message creation and delivery — not yet tested
4. ⏳ Context-switch integrity (Public → Private → Room → Public) — not yet tested

**Immediate Next Steps**:
1. Launch fresh server: `mvn -f server/pom.xml spring-boot:run`
2. Launch two client instances: `mvn -f client/pom.xml javafx:run` (×2)
3. Execute live test cases:
   - Send public message from client A → verify visible on B immediately
   - Send private message in active chat → verify visible without manual refresh
   - Send friend request → verify alert popup has visible text
   - Create room and test room message delivery
4. Validate all defects resolved before proceeding to Gate 2

### Gate 2 QA: Reconnect & Dedupe (Planned)
- Test WebSocket reconnection and message deduplication after network loss
- Criteria: Messages deduplicated by ID, no gaps, smooth reconnect

## Resume Commands
```bash
# Terminal 1: Start server
mvn -f server/pom.xml spring-boot:run

# Terminal 2: Start client A
mvn -f client/pom.xml javafx:run

# Terminal 3: Start client B
mvn -f client/pom.xml javafx:run
```

Then execute live QA checklist subtests 1–4 above.

## Notes
- **Desktop QA Status**: Gate 1 (message delivery) code fixes complete, awaiting live verification with fresh client/server launch
- **Key Findings**: Private message realtime delivery requires topic-based subscriptions (`/topic/user.{username}`), not user queues alone
- **Architecture Notes**: 
  - Client uses raw STOMP over WebSocket (no external messaging library)
  - Server uses SimpMessagingTemplate for hybrid queue + topic publishing
  - 8-second roster refresh minimum acceptable latency for friend list sync
- **Test Infrastructure**: Remove-friend feature enables rapid test iteration without account re-registration
- Docker currently packages server only (no compose setup in repo).
- Production profile uses PostgreSQL settings in application-prod.properties.
- Consider moving production DB credentials to environment variables.
