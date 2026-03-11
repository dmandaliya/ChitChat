package com.chitchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.chitchat.app.User;

public class ChatServer {

    private static final int PORT = 5050;
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    // Maps username -> their ClientHandler so handlers can reach each other
    public static final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Persistent registry of all created accounts (username -> User)
    public static final ConcurrentHashMap<String, com.chitchat.app.User> registeredUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("ChitChat Server started on port " + PORT);

        // Test users — loaded once at server startup
        User a = new User("Ayden", "Sendrea", "aydsman", "123");
        User b = new User("Jon", "Jones", "jonny", "123");
        User c = new User("Bob", "Bones", "bobby", "123");
        User d = new User("Ronald", "Rones", "ron123", "123");
        registeredUsers.put(a.getUsername(), a);
        registeredUsers.put(b.getUsername(), b);
        registeredUsers.put(c.getUsername(), c);
        registeredUsers.put(d.getUsername(), d);

        // Server console — runs in background thread so it doesn't block the accept loop
        Thread console = new Thread(() -> {
            try (BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {
                String cmd;
                while ((cmd = consoleIn.readLine()) != null) {
                    if (cmd.equals("onlineusers")) {
                        System.out.println("Online: " + String.join(", ", connectedClients.keySet()));
                    } else if (cmd.equals("totalusers")) {
                        System.out.println("Registered: " + String.join(", ", registeredUsers.keySet()));
                    } else {
                        System.out.println("Unknown server command. Available: onlineusers, totalusers");
                    }
                }
            } catch (IOException e) {
                System.out.println("Server console error: " + e.getMessage());
            }
        });
        console.setDaemon(true);
        console.start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                pool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
