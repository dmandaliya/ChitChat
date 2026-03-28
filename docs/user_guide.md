# User Guide
## ChitChat — Real-Time Messaging Application
**Course:** COMP.2800 Software Development
**Term:** 2026W | University of Windsor

---

## 1. Introduction

Welcome to **ChitChat**, a real-time messaging application that lets you connect and communicate with others instantly. ChitChat supports one-on-one private conversations, group chat rooms, emoji reactions, GIF sharing, voice/video calls, and much more.

ChitChat is available in two ways:
- **Desktop App** — A native application for Windows, macOS, and Linux
- **Web App** — Accessible in any modern web browser (Chrome, Firefox, Safari, Edge)

Both interfaces offer the same core features. This guide walks you through everything you need to get started and make the most of ChitChat.

---

## 2. Getting Started

### 2.1 Accessing ChitChat

**Desktop App:**
Launch the ChitChat desktop application. The login window will open automatically.

**Web App:**
Open your browser and navigate to the ChitChat URL provided by your administrator (e.g., `http://localhost:8080` for local installations or the hosted URL for the production server).

### 2.2 Creating an Account

If you are new to ChitChat, you need to register before you can log in.

1. On the Login screen, click the **Register** tab.
2. Fill in the following fields:
   - **First Name** — Your given name
   - **Last Name** — Your family name
   - **Username** — A unique identifier (other users will search for and see this)
   - **Password** — Must be at least 8 characters, contain at least one uppercase letter, and at least one special character (e.g., `!@#$%`)
3. Click the **Register** button.
4. If successful, you will be taken directly to the main chat screen.

> **Note:** If your username is already taken, choose a different one. Usernames are unique across all ChitChat accounts.

### 2.3 Logging In

1. On the Login screen, make sure the **Login** tab is selected.
2. Enter your **Username** and **Password**.
3. Click the **Login** button.
4. You will be taken to the main chat screen.

---

## 3. The Main Interface

Once logged in, you will see the main chat screen divided into two areas:

**Sidebar (left panel)**
- Your logged-in username is displayed at the top
- Action buttons: Settings, Blocked Users, Profile, and Logout
- **Friends** section — shows your friends with their online/offline status
- **Group Rooms** section — shows chat rooms you have joined

**Main Chat Area (right panel)**
- Chat header with the current conversation name
- Message history with bubbles (your messages appear on the right, others on the left)
- Typing indicator (shows when the other person is writing)
- Input bar at the bottom for composing and sending messages

---

## 4. Messaging

### 4.1 Public Chat

The public chat is available to all connected users. It is the default view when you first log in.

- Messages sent here are visible to everyone currently using ChitChat
- Useful for announcements or general conversation

### 4.2 Starting a Private Conversation

To chat privately with a friend:

1. Find their name in the **Friends** list on the sidebar.
2. Click their name.
3. The main area will switch to your private conversation with them.
4. Type your message in the input bar at the bottom and press **Send** (or hit Enter).

Previous messages in your conversation are loaded automatically (up to the last 50).

**Read Receipts:**
When your friend reads your message, a double checkmark (✓✓) will appear below it. A single checkmark (✓) means the message was sent but not yet read.

### 4.3 Joining a Group Room

Group rooms are shared spaces where multiple people can chat together.

**To join an existing room:**
1. In the sidebar, look at the **Group Rooms** section.
2. If you are not yet a member of any rooms, you can browse available rooms (ask your administrator or a friend to share the room name).
3. Once joined, the room appears in your sidebar. Click its name to open the conversation.

**To create a new room:**
1. Click the **+ New** button in the Group Rooms section.
2. Enter a room name (required) and an optional description.
3. Click Create. You are automatically added as the first member.
4. Share the room name with others so they can join.

**To leave a room:**
Click the **Leave** button in the chat header while viewing that room.

### 4.4 Sending Images and Files

To attach an image or file to your message:

1. Click the **📎 (paperclip)** icon in the input bar.
2. A file picker will open. Select the image or file you want to share.
3. It will be attached to your next message.

### 4.5 Sending GIFs

1. Click the **GIF** button in the input bar.
2. Type a search term in the GIF search box (e.g., "funny", "hello").
3. Click on the GIF you want to send. It will be sent immediately.

### 4.6 Voice Messages

1. Click the **🎤 (microphone)** button in the input bar to start recording.
2. Speak your message.
3. Click the button again to stop recording and send the voice note.

### 4.7 Message Reactions

To react to a message with an emoji:

1. Hover over or tap the message you want to react to.
2. Click the emoji/reaction icon that appears.
3. Select an emoji from the picker.
4. Your reaction will appear below the message, visible to everyone in the conversation.

---

## 5. Friends

### 5.1 Searching for Users

1. In the sidebar, click the **Edit** button next to the Friends section (or look for a search/add option).
2. Type the username or name of the person you are looking for.
3. Results will appear as you type.

### 5.2 Sending a Friend Request

1. Find the user via search (see above).
2. Click **Add Friend** next to their name.
3. The request is sent. They will see it in their Friend Requests panel.

### 5.3 Accepting or Rejecting a Friend Request

1. Click the **Req** button in the sidebar to open Friend Requests.
2. You will see a list of pending incoming requests.
3. Click **Accept** to add the person as a friend, or **Reject** to decline.

### 5.4 Removing a Friend

1. Find the friend in your Friends list.
2. Click the **Edit** button or right-click their name.
3. Select **Remove Friend**.
4. They will be removed from your list (and you from theirs).

### 5.5 Online Status

Friends who are currently logged in will have a **green dot** next to their name. Those who are offline will not have a dot. You can see when a friend was last active in some views.

---

## 6. Voice and Video Calls

ChitChat supports voice and video calls between users.

### 6.1 Starting a Call

1. Open a private conversation with a friend.
2. Click the **📞 Call** button in the chat header.
3. Your friend will receive an incoming call notification.
4. Once they accept, the call begins.

### 6.2 Accepting or Rejecting an Incoming Call

When someone calls you:
- An incoming call notification will appear on screen.
- Click **Accept** to answer the call.
- Click **Reject** to decline.

### 6.3 Ending a Call

Click the **End Call** button (🔇 or red phone icon) in the chat header at any time to hang up.

> **Note:** Full audio/video capability (live camera and microphone) is available in the **web browser** version. The desktop app supports call signalling and notifications.

---

## 7. Your Profile

### 7.1 Editing Your Profile

1. Click the **👤 (person)** icon in the sidebar.
2. Your profile page will open, showing your name, username, and avatar (initials).
3. You can:
   - Set your **Status**: Online, Away, Busy, or Offline
   - Write or edit your **Bio** (a short description others can see)
4. Click **Save** to apply your changes.

### 7.2 Adjusting Preferences

1. Click the **⚙ (gear/settings)** icon in the sidebar.
2. The preferences panel will open with the following options:

| Preference | Options |
|---|---|
| Dark Mode | Toggle on/off |
| Font Size | Small, Medium, Large |
| Chat Bubble Color | Blue, Teal, Orange, Pink |
| Notifications | Enable/disable desktop notifications |
| Show Read Receipts | Toggle on/off |
| Online Status Visibility | Show/hide your online status to others |
| Last Seen Visibility | Show/hide your last active time |

3. Changes take effect immediately.

---

## 8. Privacy and Safety

### 8.1 Blocking a User

If someone is bothering you, you can block them:

1. Click the **⛔ (blocked users)** icon in the sidebar.
2. Search for the user you want to block.
3. Click **Block**.
4. Blocked users cannot send you messages or see your status.

### 8.2 Unblocking a User

1. Click the **⛔ (blocked users)** icon.
2. Your list of blocked users will appear.
3. Find the user and click **Unblock**.

### 8.3 Privacy Settings

In the Preferences panel (see Section 7.2), you can control:
- Whether other users see you as online
- Whether others can see your last-seen time

---

## 9. Troubleshooting

| Problem | What to Try |
|---|---|
| **Cannot connect to the server** | Check your internet connection. Make sure the server URL is correct. Contact your administrator if the issue persists. |
| **Login button does nothing** | Double-check your username and password. Passwords are case-sensitive. |
| **"Username already taken" on registration** | Choose a different username — all usernames must be unique. |
| **Messages are not appearing in real time** | Your WebSocket connection may have dropped. Try refreshing the page (web) or restarting the app (desktop). The app will attempt to reconnect automatically. |
| **GIF search not working** | GIF search requires an active internet connection. Check your network. |
| **Cannot see the video in a call** | Make sure your browser has permission to access your camera and microphone. In Chrome: click the camera icon in the address bar to grant permissions. |
| **Friend request not showing up** | Ask your friend to refresh or re-open the Friend Requests panel. |
| **Dark mode not applying** | Save preferences and restart the chat view. Some theme changes require re-entering the chat. |

---

*For additional help, please contact your system administrator or the development team.*
