package com.chitchat.server;

import java.io.*;
import java.net.Socket;

import com.chitchat.app.User;
import com.chitchat.app.LoginService;
import com.chitchat.app.FriendService;

public class ClientHandler implements Runnable {

    private final Socket socket;
    User user;
    FriendService friend;
    LoginService loginUser;

    // Constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        user = new User(); // or later assign from login info
        friend = new FriendService(user); // bottom comment applies here too
        loginUser = new LoginService(user); // will eventually make static for efficiency

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

                if (message.startsWith("login")) {
                    loginUser.login(); // for testing purposes it asks login info in server
                    out.println("LOGIN SUCCESS");
                }
                if (message.startsWith("logout")) {
                    loginUser.logout();
                    out.println("LOGOUT SUCCESS");
                }
                else {
                    out.println("UNKNOWN COMMAND");
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
