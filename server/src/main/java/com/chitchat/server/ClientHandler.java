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
    LoginService login;

    // Constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        user = new User(); // or later assign from login info
        friend = new FriendService(user);
        login = new LoginService(user);

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("CONNECTED");

            // Recieving from client
            String line;
            while ((line = in.readLine()) != null) {
                // parse msg from client
                System.out.println("From " + socket + ": " + line);
                out.println("RECEIVED: " + line);
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
