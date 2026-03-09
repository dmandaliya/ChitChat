package com.chitchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 5050;
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    // Maps username -> their ClientHandler so handlers can reach each other
    public static final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Persistent registry of all created accounts (username -> User)
    public static final ConcurrentHashMap<String, com.chitchat.app.User> registeredUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("ChitChat Server started on port " + PORT);

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
