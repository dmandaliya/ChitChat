package com.chitchat.server;

import java.io.*;
import java.net.Socket;

import com.chitchat.app.User;
import com.chitchat.app.LoginService;
import com.chitchat.app.FriendService;
import com.chitchat.app.EncryptionService;
import com.chitchat.app.Conversation;
import com.chitchat.app.Message;
import com.chitchat.app.MessageHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private final Socket socket;
    PrintWriter out; // package-accessible so other handlers can send messages to this client
    User user;
    FriendService friend;
    LoginService login;

    // Constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // Called by another handler to deliver a message to this client
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    // Add this handler to the connected clients map for the given username
    private static void addConnectedClient(String username, ClientHandler handler) {
        ChatServer.connectedClients.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    // Remove this handler from the connected clients map; removes the key if no handlers remain
    private static void removeConnectedClient(String username, ClientHandler handler) {
        List<ClientHandler> handlers = ChatServer.connectedClients.get(username);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) ChatServer.connectedClients.remove(username);
        }
    }

    // Send a message to all active sessions of a user
    private static void deliverToUser(String username, String message) {
        List<ClientHandler> handlers = ChatServer.connectedClients.get(username);
        if (handlers != null) {
            for (ClientHandler h : handlers) h.sendMessage(message);
        }
    }

    // Check if a user has any active sessions
    private static boolean isOnline(String username) {
        List<ClientHandler> handlers = ChatServer.connectedClients.get(username);
        return handlers != null && !handlers.isEmpty();
    }

    @Override
    public void run() {
        user = new User();
        friend = new FriendService(user);
        login = new LoginService(user);


        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = writer;
            out.println("CONNECTED");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("From " + socket + ": " + line);

                if (line.startsWith("register ")) {
                    handleRegister(line);

                } else if (line.startsWith("login ")) {
                    handleLogin(line);

                } else if (line.startsWith("friend ")) {
                    handleFriendRequest(line.substring(7).trim());

                } else if (line.equals("logout")) {
                    handleLogout();

                } else if (line.startsWith("createchat ")) {
                    handleCreateChat(line);

                } else if (line.startsWith("chat ")) {
                    handleSendMessage(line);

                } else if (line.equals("mychats")) {
                    handleMyChats();

                } else if (line.startsWith("history ")) {
                    handleHistory(line);

                } else if (line.startsWith("accept ")) {
                    handleAcceptRequest(line.substring(7).trim());

                } else if (line.startsWith("decline ")) {
                    handleDeclineRequest(line.substring(8).trim());

                } else if (line.equals("users")) {
                    handleUsers();

                } else if (line.equals("friends")) {
                    handleFriends();

                } else if (line.startsWith("unfriend ")) {
                    handleUnfriend(line.substring(9).trim());

                } else if (line.equals("pendingrequests")) {
                    handlePendingRequests();

                } else if (line.startsWith("addtochat ")) {
                    handleAddToChat(line);

                } else if (line.equals("whoami")) {
                    handleWhoAmI();

                } else if (line.equals("profile") || line.startsWith("profile ")) {
                    handleProfile(line);

                } else if (line.startsWith("setbio ")) {
                    handleSetBio(line.substring(7).trim());

                } else if (line.startsWith("setstatus ")) {
                    handleSetStatus(line.substring(10).trim());

                } else if (line.startsWith("leavechat ")) {
                    handleLeaveChat(line);

                } else if (line.startsWith("block ")) {
                    handleBlock(line.substring(6).trim());

                } else if (line.startsWith("unblock ")) {
                    handleUnblock(line.substring(8).trim());

                } else if (line.equals("blocklist")) {
                    handleBlockList();

                } else if (line.equals("help")) {
                    handleHelp();

                } else {
                    out.println("Unknown command. Type 'help' for a list of commands.");
                }
                out.println("END"); // signals to client that this response is complete
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            if (login.isLoggedIn()) {
                removeConnectedClient(user.getUsername(), this);
                login.logout();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // register <fname> <lname> <username> <password>
    private void handleRegister(String line) {
        if (login.isLoggedIn()) {
            out.println("You must logout before registering a new account.");
            return;
        }
        String[] parts = line.split(" ");
        if (parts.length < 5) {
            out.println("Usage: register <fname> <lname> <username> <password>");
            return;
        }
        String fname    = parts[1];
        String lname    = parts[2];
        String username = parts[3];
        String password = parts[4];

        if (ChatServer.registeredUsers.containsKey(username)) {
            out.println("Username already taken.");
            return;
        }

        // Password validation — collect all failures and report at once
        List<String> pwErrors = new ArrayList<>();
        if (password.length() < 8)
            pwErrors.add("at least 8 characters");
        if (!password.chars().anyMatch(Character::isUpperCase))
            pwErrors.add("at least one uppercase letter");
        if (!password.chars().anyMatch(c -> !Character.isLetterOrDigit(c)))
            pwErrors.add("at least one symbol");
        if (!pwErrors.isEmpty()) {
            out.println("Password must have: " + String.join(", ", pwErrors) + ".");
            return;
        }

        // Build and store the new user
        User newUser = new User(fname, lname);
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setNewAccount(false);
        ChatServer.registeredUsers.put(username, newUser);

        // Send confirmation to client
        out.println("New user created.");
        out.println("Name: " + fname + " " + lname);
        out.println("Username: " + username);

        // Auto-login after register
        loginUser(newUser);
        out.println("Logged in (" + username + ")");
        printServerStatus();
    }

    // login <username> <password>
    private void handleLogin(String line) {
        if (login.isLoggedIn()) {
            out.println("Error: User already logged in.");
            return;
        }

        String[] parts = line.split(" ");
        if (parts.length < 3) {
            out.println("Usage: login <username> <password>");
            return;
        }
        String username = parts[1];
        String password = parts[2];

        User existing = ChatServer.registeredUsers.get(username);
        if (existing == null) {
            out.println("User does not exist.");
            return;
        }

        if (!EncryptionService.hashPassword(password).equals(existing.getHashedPassword())) {
            out.println("Incorrect password.");
            return;
        }

        loginUser(existing);
        out.println("Logged in (" + username + ")");
        printServerStatus();
    }

    // Shared logic for wiring up services and marking user as logged in
    private void loginUser(User u) {
        user = u;
        friend = new FriendService(user);
        login = new LoginService(user);
        login.login(); // updates LoginService state (currentUser), prints to server console
        addConnectedClient(user.getUsername(), this);

        // Notify user of anything that happened while offline
        List<User> pending = user.getPendingRequests();
        List<Integer> newChats = user.getPendingChatNotifications();
        if (!pending.isEmpty() || !newChats.isEmpty()) {
            out.println("While you were offline:");
            if (!pending.isEmpty()) {
                out.println("Friend requests: " + pending.size());
                for (User requester : pending) {
                    out.println("  - " + requester.getUsername() + " sent you a friend request.");
                }
            }
            if (!newChats.isEmpty()) {
                out.println("You were added to " + newChats.size() + " new chat(s): " + newChats);
            }
        }
        user.clearPendingChatNotifications();
    }

    private void handleLogout() {
        if (!login.isLoggedIn()) {
            out.println("You are not logged in.");
            return;
        }

        String username = user.getUsername();
        removeConnectedClient(username, this);
        login.logout(); // updates LoginService state (currentUser = empty), prints to server console
        printServerStatus();

        // Reset to a fresh unauthenticated state
        user = new User();
        friend = new FriendService(user);
        login = new LoginService(user);

        out.println("Logged out.");
    }

    // friend <username>
    private void handleFriendRequest(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in to send friend requests.");
            return;
        }

        String myUsername = user.getUsername();

        if (targetUsername.equals(myUsername)) {
            out.println("You can't friend yourself.");
            return;
        }

        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }

        // Block checks
        if (user.hasBlocked(targetUser)) {
            out.println("You have blocked " + targetUsername + ". Unblock them first.");
            return;
        }
        if (targetUser.hasBlocked(user)) {
            out.println("You cannot send a friend request to " + targetUsername + ".");
            return;
        }

        // Already friends check
        if (user.getFriendList().contains(targetUser)) {
            out.println("You are already friends with " + targetUsername + ".");
            return;
        }

        // Duplicate pending request check
        if (targetUser.getPendingRequests().contains(user)) {
            out.println("You already sent a friend request to " + targetUsername + ".");
            return;
        }

        // If target already sent us a request, accept it — mutual add via FriendService
        if (user.getPendingRequests().contains(targetUser)) {
            if (isOnline(targetUsername)) {
                // Use first session to call acceptRequest, then notify all sessions
                ChatServer.connectedClients.get(targetUsername).get(0).friend.acceptRequest(user);
                deliverToUser(targetUsername, "You and " + myUsername + " are now friends!");
            } else {
                // Accept locally — target will see it when they log in
                friend.acceptRequest(targetUser);
            }
            out.println("You and " + targetUsername + " are now friends!");
        } else {
            // Queue the request on the target user
            friend.addFriend(targetUser);
            if (isOnline(targetUsername)) {
                deliverToUser(targetUsername, myUsername + " has sent you a friend request.");
            }
            System.out.println(myUsername + " sent a friend request to " + targetUsername);
            out.println("Friend request sent to " + targetUsername + ".");
        }
    }

    // createchat <user1> <user2> ...
    private void handleCreateChat(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in to create a chat.");
            return;
        }

        String[] parts = line.split(" ");
        if (parts.length < 2) {
            out.println("Usage: createchat <user1> <user2> ...");
            return;
        }

        List<User> members = new ArrayList<>();
        members.add(user); // include the sender

        for (int i = 1; i < parts.length; i++) {
            String targetUsername = parts[i];

            if (targetUsername.equals(user.getUsername())) {
                out.println("You cannot add yourself to a chat.");
                return;
            }

            User target = ChatServer.registeredUsers.get(targetUsername);
            if (target == null || !user.getFriendList().contains(target)) {
                out.println(targetUsername + " is not in your friends list.");
                return;
            }

            if (members.contains(target)) {
                out.println(targetUsername + " is already in this chat.");
                return;
            }

            members.add(target);
        }

        Conversation convo = MessageHandler.createConversation(members);
        if (convo != null) {
            out.println("Chat created! Conversation ID: " + convo.getId());
            System.out.println("New Chat Created | ID: " + convo.getId() + " | Members: " + MessageHandler.getMembers(convo.getId()));
            // Queue notification for offline members
            for (User member : members) {
                if (!member.getUsername().equals(user.getUsername()) && !isOnline(member.getUsername())) {
                    member.addPendingChatNotification(convo.getId());
                }
            }
        }
    }

    // chat <convo_id> <message>
    private void handleSendMessage(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in to send messages.");
            return;
        }

        String[] parts = line.split(" ", 3);
        if (parts.length < 3) {
            out.println("Usage: chat <convo_id> <message>");
            return;
        }

        int convoId;
        try {
            convoId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("Invalid conversation ID.");
            return;
        }

        String content = parts[2];
        Message msg = MessageHandler.sendMessage(user, convoId, content);
        if (msg == null) {
            out.println("Could not send message. Conversation not found or you are not a member.");
            return;
        }

        // Deliver to all members in the conversation (skip blocked users)
        String formatted = "[" + user.getUsername() + "]: " + content;
        List<String> offline = new ArrayList<>();
        for (String username : MessageHandler.getMembers(convoId)) {
            User member = ChatServer.registeredUsers.get(username);
            if (member != null && (member.hasBlocked(user) || user.hasBlocked(member))) continue;
            if (isOnline(username)) {
                deliverToUser(username, formatted);
            } else if (!username.equals(user.getUsername())) {
                offline.add(username);
            }
        }
        if (!offline.isEmpty()) {
            out.println("(Offline and did not receive message: " + String.join(", ", offline) + ")");
        }
    }

    // mychats
    private void handleMyChats() {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        List<Integer> ids = user.getConversationIds();
        if (ids.isEmpty()) {
            out.println("You have no chats.");
            return;
        }
        out.println("Your chats:");
        for (int id : ids) {
            out.println("  ID: " + id + " | Members: " + MessageHandler.getMembers(id));
        }
    }

    // history <convo_id>
    private void handleHistory(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            out.println("Usage: history <convo_id>");
            return;
        }
        int convoId;
        try {
            convoId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("Invalid conversation ID.");
            return;
        }
        Conversation convo = MessageHandler.getConversation(convoId);
        if (convo == null || !convo.hasMember(user.getUsername())) {
            out.println("Conversation not found or you are not a member.");
            return;
        }
        List<Message> messages = convo.getMessages();
        if (messages.isEmpty()) {
            out.println("No messages yet.");
            return;
        }
        for (Message msg : messages) {
            out.println(msg.getSender() + ": " + msg.getContent());
        }
    }

    // accept <username>
    private void handleAcceptRequest(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }
        if (!user.getPendingRequests().contains(targetUser)) {
            out.println("No pending request from " + targetUsername + ".");
            return;
        }
        friend.acceptRequest(targetUser);
        out.println("You and " + targetUsername + " are now friends!");
        deliverToUser(targetUsername, "You and " + user.getUsername() + " are now friends!");
    }

    // decline <username>
    private void handleDeclineRequest(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }
        if (!user.getPendingRequests().contains(targetUser)) {
            out.println("No pending request from " + targetUsername + ".");
            return;
        }
        user.removePendingRequest(targetUser);
        out.println("Declined friend request from " + targetUsername + ".");
    }

    // users
    private void handleUsers() {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        out.println("Online: " + String.join(", ", ChatServer.connectedClients.keySet()));
    }

    // unfriend <username>
    private void handleUnfriend(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }
        if (!user.getFriendList().contains(targetUser)) {
            out.println(targetUsername + " is not in your friends list.");
            return;
        }
        friend.removeFriend(targetUser);
        targetUser.getFriendList().remove(user);
        out.println("You unfriended " + targetUsername + ".");
        deliverToUser(targetUsername, user.getUsername() + " unfriended you.");
    }

    // friends
    private void handleFriends() {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        List<User> friends = user.getFriendList();
        if (friends.isEmpty()) {
            out.println("You have no friends yet.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (User f : friends) names.add(f.getUsername());
        out.println("Friends: " + String.join(", ", names));
    }

    // pendingrequests
    private void handlePendingRequests() {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        List<User> pending = user.getPendingRequests();
        if (pending.isEmpty()) {
            out.println("No pending friend requests.");
            return;
        }
        out.println("Pending friend requests: " + pending.size());
        for (User requester : pending) {
            out.println("  - " + requester.getUsername());
        }
    }

    // addtochat <convo_id> <username>
    private void handleAddToChat(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        String[] parts = line.split(" ");
        if (parts.length < 3) {
            out.println("Usage: addtochat <convo_id> <username>");
            return;
        }
        int convoId;
        try {
            convoId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("Invalid conversation ID.");
            return;
        }
        Conversation convo = MessageHandler.getConversation(convoId);
        if (convo == null || !convo.hasMember(user.getUsername())) {
            out.println("Conversation not found or you are not a member.");
            return;
        }
        String targetUsername = parts[2];
        User target = ChatServer.registeredUsers.get(targetUsername);
        if (target == null || !user.getFriendList().contains(target)) {
            out.println(targetUsername + " is not in your friends list.");
            return;
        }
        if (convo.hasMember(targetUsername)) {
            out.println(targetUsername + " is already in this chat.");
            return;
        }
        convo.addMember(target);
        target.addConversationId(convoId);
        out.println(targetUsername + " added to chat " + convoId + ".");
        if (isOnline(targetUsername)) {
            deliverToUser(targetUsername, user.getUsername() + " added you to chat " + convoId + ".");
        } else {
            target.addPendingChatNotification(convoId);
        }
        System.out.println(user.getUsername() + " added " + targetUsername + " to chat " + convoId);
    }

    // whoami
    private void handleWhoAmI() {
        if (!login.isLoggedIn()) {
            out.println("You are not logged in.");
            return;
        }
        out.println("Logged in as: " + user.getUsername());
    }

    // profile [username]
    private void handleProfile(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        String[] parts = line.split(" ", 2);
        User target = parts.length > 1 ? ChatServer.registeredUsers.get(parts[1].trim()) : user;
        if (target == null) {
            out.println("User not found.");
            return;
        }
        if (target.hasBlocked(user)) {
            out.println("User not found.");
            return;
        }
        out.println("--- Profile: " + target.getUsername() + " ---");
        out.println("Name:   " + target.getFname() + " " + target.getLname());
        out.println("Bio:    " + (target.getProfile().getBio().isEmpty() ? "(none)" : target.getProfile().getBio()));
        out.println("Status: " + target.getProfile().getStatus());
    }

    // setbio <text>
    private void handleSetBio(String bio) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        user.getProfile().setBio(bio);
        out.println("Bio updated.");
    }

    // setstatus <text>
    private void handleSetStatus(String status) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        user.getProfile().setStatus(status);
        out.println("Status updated.");
    }

    // leavechat <convo_id>
    private void handleLeaveChat(String line) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            out.println("Usage: leavechat <convo_id>");
            return;
        }
        int convoId;
        try {
            convoId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("Invalid conversation ID.");
            return;
        }
        Conversation convo = MessageHandler.getConversation(convoId);
        if (convo == null || !convo.hasMember(user.getUsername())) {
            out.println("Conversation not found or you are not a member.");
            return;
        }
        convo.getMembers().remove(user);
        user.getConversationIds().remove(Integer.valueOf(convoId));
        out.println("You left chat " + convoId + ".");
        // Notify remaining members
        String formatted = user.getUsername() + " has left the chat.";
        for (String username : MessageHandler.getMembers(convoId)) {
            deliverToUser(username, formatted);
        }
    }

    // block <username>
    private void handleBlock(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        if (targetUsername.equals(user.getUsername())) {
            out.println("You can't block yourself.");
            return;
        }
        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }
        if (user.hasBlocked(targetUser)) {
            out.println("You have already blocked " + targetUsername + ".");
            return;
        }
        // Unfriend both sides
        user.getFriendList().remove(targetUser);
        targetUser.getFriendList().remove(user);
        // Remove pending requests both ways
        user.removePendingRequest(targetUser);
        targetUser.removePendingRequest(user);
        // Block
        user.blockUser(targetUser);
        out.println("Blocked " + targetUsername + ". They have been unfriended.");
    }

    // unblock <username>
    private void handleUnblock(String targetUsername) {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        User targetUser = ChatServer.registeredUsers.get(targetUsername);
        if (targetUser == null) {
            out.println("User does not exist.");
            return;
        }
        if (!user.hasBlocked(targetUser)) {
            out.println("You have not blocked " + targetUsername + ".");
            return;
        }
        user.unblockUser(targetUser);
        out.println("Unblocked " + targetUsername + ".");
    }

    // blocklist
    private void handleBlockList() {
        if (!login.isLoggedIn()) {
            out.println("You must be logged in.");
            return;
        }
        List<User> blocked = user.getBlockedUsers();
        if (blocked.isEmpty()) {
            out.println("Your block list is empty.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (User u : blocked) names.add(u.getUsername());
        out.println("Blocked users: " + String.join(", ", names));
    }

    // help
    private void handleHelp() {
        out.println("--- Available Commands ---");
        out.println("register <fname> <lname> <username> <password>");
        out.println("login <username> <password>");
        out.println("logout");
        out.println("whoami");
        out.println("profile [username]");
        out.println("setbio <text>");
        out.println("setstatus <text>");
        out.println("friend <username>");
        out.println("unfriend <username>");
        out.println("accept <username>");
        out.println("decline <username>");
        out.println("friends");
        out.println("pendingrequests");
        out.println("block <username>");
        out.println("unblock <username>");
        out.println("blocklist");
        out.println("users");
        out.println("createchat <user1> <user2> ...");
        out.println("addtochat <convo_id> <username>");
        out.println("leavechat <convo_id>");
        out.println("mychats");
        out.println("history <convo_id>");
        out.println("chat <convo_id> <message>");
        out.println("help");
    }

    private void printServerStatus() {
        String allUsers = String.join(", ", ChatServer.registeredUsers.keySet());
        String onlineUsers = String.join(", ", ChatServer.connectedClients.keySet());
        System.out.println("All users: " + allUsers);
        System.out.println("All online users: " + onlineUsers);
    }
}
