package com.chitchat.server;

import java.io.*;
import java.net.Socket;

import com.chitchat.app.User;
import java.util.List;
import java.util.ArrayList;
import com.chitchat.app.LoginService;
import com.chitchat.app.FriendService;
import com.chitchat.app.UserManager;

public class ClientHandler implements Runnable {

    User clientUser = null;
    private final Socket socket;
    User user;
    User user2;
    FriendService friend;
    LoginService loginUser;
    List<User> allUsers = UserManager.getAllUsers();
    List<User> allOnlineUsers = UserManager.getAllOnlineUsers();

    // Constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // helper method to print lists
    public void printList(List<User> list, String msg) {
        System.out.println(msg + ": " + list.size());
        for (User u : list) {
            System.out.println("User: " + u.getUsername());
        }
    }

    public static List<String> parseList(String msg) {
        // Split the msg string by spaces
        String[] words = msg.split(" ");

        // Create a list to store words except the first
        List<String> result = new ArrayList<>();

        // Start from index 1 to skip the first word
        for (int i = 1; i < words.length; i++) {
            result.add(words[i]);
        }

        return result;
    }

    @Override
    public void run() {

        user = new User();
        user2 = new User();

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("CONNECTED");

            // Recieving from client
            String line;

            // parsing msg from client and storing it into String message.
            while ((line = in.readLine()) != null) {

                String message = line.trim(); // stores client msg
                List<String> parsedMsg = parseList(message);

                System.out.println("From " + socket + ": " + message);

                // logging in
                if ((message.startsWith("login") && (clientUser == null))) {
                    clientUser = user; // assigns user to client. (change 'user' to the user u want to store to client)
                    LoginService.login(user, parsedMsg.get(0), parsedMsg.get(1), parsedMsg.get(2), parsedMsg.get(3)); // for testing purposes it asks login info in server
                    UserManager.addOnlineUser(user); // adds this to list of online users.
                    out.println("LOGIN SUCCESS");
                }
                // logging out
                else if ((message.startsWith("logout") && (clientUser != null))) {
                    LoginService.logout(user);
                    clientUser = null;
                    out.println("LOGOUT SUCCESS");
                }

                // adding friend
                else if ((message.startsWith("friend") && (clientUser == null))) {
                    FriendService.addFriend(user, user2); // main user adds test as friend
                    user.printList(user);
                    out.println("ADDED FRIEND");
                }
                else {
                    out.println("UNKNOWN COMMAND");
                }
                // print list of every created user
                printList(allUsers, "All users created");
            }

            // when client exits
        } catch (IOException e) {
            UserManager.removeOnlineUser(clientUser);
            printList(allOnlineUsers, "All online users:");
            System.out.println("Client disconnected: " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public User returnUser(User user) {
        return user;
    }
}
