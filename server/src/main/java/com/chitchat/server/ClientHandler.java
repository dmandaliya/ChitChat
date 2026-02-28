package com.chitchat.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("CONNECTED");

            String line;
            while ((line = in.readLine()) != null) {
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
