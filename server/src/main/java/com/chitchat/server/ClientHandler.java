package com.chitchat.server;

import java.io.*;
import java.net.Socket;

import com.chitchat.app.User;
import com.chitchat.app.LoginService;
import com.chitchat.app.FriendService;
import com.chitchat.app.EncryptionService;

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

                } else {
                    out.println("RECEIVED: " + line);
                }
                out.println("END"); // signals to client that this response is complete
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            if (login.isLoggedIn()) {
                ChatServer.connectedClients.remove(user.getUsername());
                login.logout();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // register <fname> <lname> <username> <password>
    private void handleRegister(String line) {
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
        out.println("Password: " + password);
        out.println("Hashed Password: " + newUser.getHashedPassword());

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
        ChatServer.connectedClients.put(user.getUsername(), this);
    }

    private void handleLogout() {
        if (!login.isLoggedIn()) {
            out.println("You are not logged in.");
            return;
        }

        String username = user.getUsername();
        ChatServer.connectedClients.remove(username);
        login.logout(); // updates LoginService state (currentUser = empty), prints to server console

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

        ClientHandler targetHandler = ChatServer.connectedClients.get(targetUsername);
        if (targetHandler == null) {
            out.println(targetUsername + " is not online.");
            return;
        }

        // If target already sent us a request, accept it — mutual add via FriendService
        if (user.getPendingRequests().contains(targetUser)) {
            targetHandler.friend.acceptRequest(user); // adds both to each other's friend lists
            out.println("You and " + targetUsername + " are now friends!");
            targetHandler.sendMessage("You and " + myUsername + " are now friends!");
        } else {
            // Send the request via FriendService — adds user to targetUser's pending list
            friend.addFriend(targetUser);
            targetHandler.sendMessage(myUsername + " has sent you a friend request.");
            out.println("Friend request sent to " + targetUsername + ".");
        }
    }

    private void printServerStatus() {
        String allUsers = String.join(", ", ChatServer.registeredUsers.keySet());
        String onlineUsers = String.join(", ", ChatServer.connectedClients.keySet());
        System.out.println("All users: " + allUsers);
        System.out.println("All online users: " + onlineUsers);
    }
}
