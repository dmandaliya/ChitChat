# Desktop QA Checklist (Feature Parity Branch)

Use this checklist after each successful build to validate desktop parity progress before merge.

## Pre-PR Fast Path (35-45 min)

Use this when you need a quick, high-signal run before opening a PR.

1. Gate 0 - Build and Launch Baseline (5-8 min)
	- Run `mvn clean compile`.
	- Start backend and two desktop clients.
	- Pass: Build succeeds, backend starts cleanly, both clients reach login.

2. Gate 1 - Core Messaging and Context Integrity (12-15 min)
	- Login two users.
	- Verify public, private, and room messaging in both directions.
	- Switch contexts repeatedly (public <-> private <-> room).
	- Pass: Messages stay in correct context; no duplicate spikes during switching.

3. Gate 2 - Reconnect and Dedupe (6-8 min)
	- Stop backend briefly, then restart.
	- Verify clients recover and send/receive after reconnect.
	- Pass: No client restart needed and no duplicate message rendering.

4. Gate 3 - Typing and Seen Receipts (4-5 min)
	- In private chat, verify typing indicator appears for the other user.
	- Send message and verify receipt transitions to `Seen`.
	- Pass: Typing appears only in active private context; seen state updates correctly.

5. Gate 4 - Persistence and Social Controls (6-9 min)
	- Update profile and preferences, then re-open/re-login to confirm persistence.
	- Send and accept/reject friend request; verify list refresh.
	- Block and unblock a user.
	- Pass: Settings persist and friend/block flows update correctly.

Go/No-Go rule:
- Block PR if any Gate 0-3 check fails.
- Gate 4 failures require a documented known issue with repro steps.

## 1) Build Validation

Run from project root:

```powershell
mvn clean compile
```

Pass criteria:
- Build finishes with `BUILD SUCCESS`.
- No new compile errors across `shared`, `server`, or `client`.

## 2) Runtime Setup

Open 3 terminals from project root.

Terminal A (backend):

```powershell
mvn -pl server -am spring-boot:run
```

Terminal B (desktop client #1):

```powershell
mvn -pl client -am javafx:run
```

Terminal C (desktop client #2):

```powershell
mvn -pl client -am javafx:run
```

Pass criteria:
- Backend starts without fatal exceptions.
- Both desktop windows open and reach login screen.

## 3) Test Accounts

Use two users for two-client testing, for example:
- `alice1`
- `bob1`

If users do not exist, register them from the login screen.

## 4) Functional Smoke Suite

### 4.1 Authentication
- Login both users.
- Logout and login again.

Pass criteria:
- Successful scene transitions.
- No client crash during login/logout.

### 4.2 Friends and Requests
- From user A, send friend request to user B.
- On user B, open `Req` dialog and accept or reject.
- Verify friend list refreshes.

Pass criteria:
- Request action returns success.
- Friend list reflects accepted relationship.

### 4.3 Public, Private, Room Messaging
- Send public message; both clients receive.
- Open private chat A <-> B; send both directions.
- Create a room; open the room and send messages.

Pass criteria:
- Messages appear in the correct conversation context.
- No cross-thread leakage (private message should not appear in public stream).

### 4.4 History Loading
- Switch between public, private, and room contexts.
- Confirm historical messages load each time.

Pass criteria:
- History populates consistently after context switch.
- No duplicate rendering spikes from repeated switching.

### 4.5 Typing and Read Receipts (Private)
- Type in private chat from user A.
- Observe typing indicator on user B.
- Send a private message to B and confirm A receives status update to `Seen`.

Pass criteria:
- Typing indicator appears in active private context.
- Read receipt transitions from sent state to seen state.

### 4.6 Profile and Preferences
- Open profile, edit status/bio, save.
- Re-open profile and verify persisted values.
- Open preferences, change options, save.

Pass criteria:
- Save confirms without error.
- Values persist on subsequent reload.

### 4.7 Blocked Users
- Open blocked users dialog.
- Block a username.
- Unblock the same username.

Pass criteria:
- Entry appears after block and is removable after unblock.

## 5) Quick Troubleshooting

If behavior is inconsistent:
1. Re-run clean compile:

```powershell
mvn clean compile
```

2. Restart backend and both desktop clients.
3. Re-test the failing scenario with fresh login sessions.

## 6) Optional Audit Evidence

Capture for each run:
- Branch name and commit hash.
- Date/time of test run.
- Pass/fail by section.
- Notes for regressions and reproduction steps.

Suggested table format:

| Area | Result | Notes |
|---|---|---|
| Auth | Pass/Fail | |
| Friends/Requests | Pass/Fail | |
| Messaging | Pass/Fail | |
| History | Pass/Fail | |
| Typing/Receipts | Pass/Fail | |
| Profile/Preferences | Pass/Fail | |
| Blocked Users | Pass/Fail | |
