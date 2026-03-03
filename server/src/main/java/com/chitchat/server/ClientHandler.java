package com.chitchat.server;

import java.io.*;
import java.net.Socket;

import com.chitchat.app.User;
import java.util.List;
import com.chitchat.app.LoginService;
import com.chitchat.app.FriendService;
import com.chitchat.app.UserManager;

public class ClientHandler implements Runnable {

    User clientUser = null;
    private final Socket socket;
    User user;
    User test; // user for testing
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

    @Override
    public void run() {

        // test user
        test = new User("Fname", "Lname");
        test.setUsername("tester123");
        test.setPassword("secret678");
        test.setNewAccount(false);
        UserManager.addUser(test);

        user = new User(); // or later assign from login info

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

                System.out.println("From " + socket + ": " + message);

                // logging in
                if ((message.startsWith("login") && (clientUser == null))) {
                    clientUser = user; // assigns user to client. (change 'user' to the user u want to store to client)
                    LoginService.login(user, "Ayden", "Sendrea", "aydsman", "passcode123"); // for testing purposes it asks login info in server
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
                    FriendService.addFriend(user, test); // main user adds test as friend
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
