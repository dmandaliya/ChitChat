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

    // Constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
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
        friend = new FriendService(user); // bottom comment applies here too
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
                if (message.startsWith("login")) {
                    clientUser = user; // assigns user to client. (change 'user' to the user u want to store to client)
                    LoginService.login(user, "Ayden", "Sendrea", "aydsman", "passcode123"); // for testing purposes it asks login info in server
                    out.println("LOGIN SUCCESS");
                }
                // logging out
                else if (message.startsWith("logout")) {
                    LoginService.logout(user);
                    out.println("LOGOUT SUCCESS");
                }

                // adding friend
                else if (message.startsWith("friend")) {
                    friend.addFriend(test); // main user adds test as friend
                    user.printList(user);
                    out.println("ADDED FRIEND");
                }
                else {
                    out.println("UNKNOWN COMMAND");
                }
                // print list of every created user
                List<User> allUsers = UserManager.getAllUsers();
                System.out.println("Current total users: " + allUsers.size());
                for (User u : allUsers) {
                    System.out.println("User: " + u.getUsername());
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public User returnUser(User user) {
        return user;
    }
}
