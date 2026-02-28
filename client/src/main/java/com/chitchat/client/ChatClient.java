package com.chitchat.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

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

            while (true) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                out.println(message);

                String response = in.readLine();
                System.out.println("Server: " + response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
