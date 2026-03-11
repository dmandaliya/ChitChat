package com.chitchat.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class ChatClient {

    private static final String SERVER = "localhost";
    private static final int PORT = 5050;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to server");
            System.out.println(in.readLine());

            // Released by reader thread each time it receives "END"
            Semaphore endSignal = new Semaphore(0);

            Thread reader = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        if (response.equals("END")) {
                            endSignal.release();
                        } else {
                            System.out.println("Server: " + response);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            reader.setDaemon(true);
            reader.start();

            while (true) {
                String message = scanner.nextLine();
                out.println(message);

                // Wait for server to finish responding before showing menu again
                endSignal.acquire();
                System.out.println();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}