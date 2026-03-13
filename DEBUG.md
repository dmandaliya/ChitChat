# ChitChat - Command Reference

## Client Commands

| Command | Example | Description |
|---------|---------|-------------|
| register | `register Fname Lname username password` | Create a new account |
| login | `login username password` | Log in |
| logout | `logout` | Log out |
| whoami | `whoami` | Show your current logged-in username |
| friend | `friend username` | Send a friend request |
| unfriend | `unfriend username` | Remove a friend |
| accept | `accept username` | Accept a friend request |
| decline | `decline username` | Decline a friend request |
| friends | `friends` | List your friends |
| pendingrequests | `pendingrequests` | List incoming friend requests |
| users | `users` | List online users |
| profile | `profile` or `profile <username>` | View your own or another user's profile |
| setbio | `setbio <text>` | Set your bio |
| setstatus | `setstatus <text>` | Set your status |
| block | `block <username>` | Block a user (unfriends them, prevents all contact) |
| unblock | `unblock <username>` | Unblock a user |
| blocklist | `blocklist` | See who you have blocked |
| createchat | `createchat user1 user2 ...` | Create a group chat (friends only) |
| leavechat | `leavechat <convo_id>` | Leave a conversation |
| addtochat | `addtochat <convo_id> <username>` | Add a friend to an existing chat |
| mychats | `mychats` | List your conversations |
| history | `history <convo_id>` | View message history for a conversation |
| chat | `chat <convo_id> <message>` | Send a message to a conversation |

## Server Console Commands

| Command | Description |
|---------|-------------|
| onlineusers | Show all currently online users |
| totalusers | Show all registered users |