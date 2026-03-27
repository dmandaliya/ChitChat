# Cross-Platform QA Checklist (Desktop ↔ Web)

Test parity between desktop app (JavaFX) and web app. Both platforms connect to **Render backend**: https://chitchat-moy8.onrender.com/

## Test Users

- **alice_web**: Web platform user
- **bob_desktop**: Desktop platform user
- **charlie_either**: For additional multi-user tests

Register as needed from respective platforms.

---

## Phase 1: Authentication & Basic Messaging (15 min)

### 1.1 User Registration
- [ ] Desktop: Register new user, verify email confirmation
- [ ] Web: Register new user, verify email confirmation
- [ ] Both: Login successfully with new credentials

### 1.2 Public Message Sync
- [ ] Desktop (bob_desktop): Send public message "Hello from Desktop"
- [ ] Web (alice_web): Verify message appears in public chat
- [ ] Web (alice_web): Send public message "Hi from Web"
- [ ] Desktop (bob_desktop): Verify message appears in public chat
- [ ] Both: Messages appear in correct order with timestamps

### 1.3 Private Message Sync
- [ ] Desktop (bob_desktop): Open private chat with alice_web, send "Private from desktop"
- [ ] Web (alice_web): Verify message appears in private inbox
- [ ] Web (alice_web): Reply "Got it from web"
- [ ] Desktop (bob_desktop): Verify reply appears in conversation
- [ ] Both: Conversation history loads correctly when reopened

**Pass Criteria**: Messages sync within 1 second both directions, timestamps consistent

---

## Phase 2: Real-time State Sync (10 min)

### 2.1 Typing Indicators
- [ ] Desktop: Start typing in private chat with alice_web, observe web for indicator
- [ ] Web: See "bob_desktop is typing..." indicator appears
- [ ] Web: Start typing in private chat with bob_desktop
- [ ] Desktop: See typing indicator for alice_web
- [ ] Both: Indicator disappears after 3 seconds of inactivity

### 2.2 Read Receipts
- [ ] Web (alice_web): Send private message to bob_desktop
- [ ] Desktop (bob_desktop): Receive and open message
- [ ] Web (alice_web): Verify receipt updates from "Sent" → "Seen"
- [ ] Desktop (bob_desktop): Send private message to alice_web
- [ ] Web (alice_web): Receive, open, verify receipt syncs

### 2.3 Online/Offline Status
- [ ] Desktop: Login, verify status shows "online" on web
- [ ] Web: See bob_desktop marked as online in friend list
- [ ] Desktop: Logout, wait 10 seconds
- [ ] Web: Verify bob_desktop status changes to "offline"

**Pass Criteria**: All state changes visible within 500ms, no stale indicators

---

## Phase 3: Social Features (12 min)

### 3.1 Friend Requests
- [ ] Web (alice_web): Send friend request to new test user charlie_either
- [ ] Desktop (charlie_either): Receive request notification, open Req dialog
- [ ] Desktop (charlie_either): Accept request
- [ ] Web (alice_web): Verify charlie appears in friend list
- [ ] Desktop (bob_desktop): Send friend request to charlie_either
- [ ] Web (charlie_either): Reject request
- [ ] Desktop (bob_desktop): Verify charlie not in friend list

### 3.2 Blocking
- [ ] Desktop (bob_desktop): Block alice_web from blocked users dialog
- [ ] Web (alice_web): Try to send message to bob_desktop, see "User blocked" error
- [ ] Desktop (bob_desktop): Unblock alice_web
- [ ] Web (alice_web): Send message again, verify succeeds

**Pass Criteria**: Friend list, request notifications, and blocks sync immediately

---

## Phase 4: Profile & Persistence (10 min)

### 4.1 Profile Updates (Desktop)
- [ ] Desktop (bob_desktop): Open profile, change bio to "Desktop test bio v1"
- [ ] Desktop (bob_desktop): Save and close profile
- [ ] Web (alice_web): Open bob_desktop's profile, verify bio shows "Desktop test bio v1"
- [ ] Desktop (bob_desktop): Close and reopen app
- [ ] Desktop (bob_desktop): Open profile, verify bio still "Desktop test bio v1"

### 4.2 Profile Updates (Web)
- [ ] Web (alice_web): Open profile, change bio to "Web test bio v1"
- [ ] Web (alice_web): Save and refresh page
- [ ] Desktop (bob_desktop): Search for alice_web, view profile, verify bio shows "Web test bio v1"
- [ ] Web (alice_web): Close browser, reopen, login, verify bio persisted

### 4.3 Preferences Sync
- [ ] Desktop (bob_desktop): Open preferences, enable dark mode, save
- [ ] Desktop (bob_desktop): Close and reopen app, verify dark mode persisted
- [ ] Web (alice_web): View doesn't expose theme toggle (expected), verify desktop preference doesn't affect web
- [ ] Desktop (bob_desktop): Disable dark mode, verify web is unaffected

**Pass Criteria**: Profile changes visible across platforms within 2 seconds

---

## Phase 5: Multi-window Synchronization (10 min)

**Setup**: Open same conversation on both platforms simultaneously

### 5.1 Same Public Chat
- [ ] Desktop + Web: Both open public chat view
- [ ] Desktop: Send message M1 "From desktop"
- [ ] Web: Verify M1 appears immediately
- [ ] Web: Send message M2 "From web"
- [ ] Desktop: Verify M2 appears immediately
- [ ] Both: Scroll to history, verify all old messages load correctly

### 5.2 Same Private Chat
- [ ] Desktop (bob_desktop) + Web (alice_web): Both open same private conversation
- [ ] Desktop: Send "D-private-1"
- [ ] Web: See message, send "W-private-1"
- [ ] Desktop: See message
- [ ] Both: Type simultaneously (both typing indicators visible)
- [ ] Both: Both send messages within 1 second
- [ ] Both: Verify no duplicates, both messages appear once each

### 5.3 Room Chat (if applicable)
- [ ] Desktop: Create room "cross-platform-test"
- [ ] Web: Search and join same room
- [ ] Desktop: Send message in room
- [ ] Web: Verify message appears
- [ ] Web: Send message in room
- [ ] Desktop: Verify message appears

**Pass Criteria**: No message duplication, all content syncs within 500ms

---

## Phase 6: Edge Cases & Robustness (15 min)

### 6.1 Rapid Message Sending
- [ ] Desktop: Send 5 messages rapidly in succession
- [ ] Web: Verify all 5 appear, none duplicated, correct order
- [ ] Web: Send 5 messages rapidly
- [ ] Desktop: Verify all 5 appear, none duplicated, correct order

### 6.2 Network Interruption (Desktop Only)
- [ ] Desktop: Close app, kill backend server
- [ ] Desktop: Reopen app (will fail to connect)
- [ ] Restart Render backend
- [ ] Desktop: Should auto-reconnect and sync messages
- [ ] Web: Remains online, no disruption
- [ ] Desktop: Verify no message loss after reconnect

### 6.3 Concurrent Updates
- [ ] Desktop: Update profile bio to "Desktop V2"
- [ ] Simultaneously, Web: Update profile bio to "Web V2"
- [ ] Both: After 2 seconds, verify final state is consistent (one value wins)
- [ ] Web: Refresh, verify sees same final value
- [ ] Desktop: Close/reopen, verify sees same final value

### 6.4 Large Message Content
- [ ] Desktop: Send message with 500+ characters of text
- [ ] Web: Verify full text appears correctly
- [ ] Web: Send message with 1000 characters
- [ ] Desktop: Verify formatting and wrapping correct

### 6.5 Image Messaging (if enabled)
- [ ] Desktop: Send image via "Attach" button
- [ ] Web: Verify image appears and renders correctly
- [ ] Web: Send image
- [ ] Desktop: Verify image appears and renders correctly
- [ ] Both: Re-open chat, verify images persist

**Pass Criteria**: System handles stress without lag, duplicates, or message loss

---

## Phase 7: Performance Baselines (Optional, 10 min)

### 7.1 Load Times
- [ ] Desktop: Measure time from login to public chat loads. Target: < 2 seconds
- [ ] Web: Measure time from login to public chat loads. Target: < 3 seconds
- [ ] Desktop: Measure time to load private conversation with 100+ messages. Target: < 3 seconds
- [ ] Web: Measure time to load same conversation. Target: < 3 seconds

### 7.2 Memory/CPU
- [ ] Desktop: Monitor CPU while sending 50 messages. Should stay < 30% peak
- [ ] Web: Inspect browser memory. Should not exceed 150MB
- [ ] Both: Idle for 5 minutes, verify no unexpected background activity

**Pass Criteria**: Response times < targets, no memory leaks

---

## Test Summary

| Phase | Pass | Fail | Notes |
|-------|------|------|-------|
| 1. Auth & Messaging | [ ] | [ ] | |
| 2. Real-time Sync | [ ] | [ ] | |
| 3. Social Features | [ ] | [ ] | |
| 4. Profile & Persistence | [ ] | [ ] | |
| 5. Multi-window Sync | [ ] | [ ] | |
| 6. Edge Cases | [ ] | [ ] | |
| 7. Performance | [ ] | [ ] | Optional |

---

## Known Issues / Blockers

(Document any issues discovered during cross-platform testing)

1. **Issue**: [Description]
   - **Platform**: Desktop / Web / Both
   - **Severity**: Critical / High / Medium / Low
   - **Reproduction**: [Steps]
   - **Status**: [Open / In Progress / Resolved]

---

## Sign-off

- **Tester**: 
- **Date**: 
- **Overall Result**: ✅ PASS / ⚠️ PASS WITH ISSUES / ❌ FAIL
- **Ready for Merge**: Yes / No / Conditional

