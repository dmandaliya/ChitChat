package com.chitchat.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClientApp extends Application {

    private static final String DEFAULT_SERVER_URL = "https://chitchat-pidj7.ondigitalocean.app";
    public static final String SERVER_URL = resolveServerUrl();

    private static String resolveServerUrl() {
        String fromProperty = System.getProperty("chitchat.server.url");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }

        String fromEnv = System.getenv("CHITCHAT_SERVER_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        // Auto-detect: prefer cloud, fall back to localhost if cloud is unreachable
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(DEFAULT_SERVER_URL + "/api/users/search?q=ping").openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 500) {
                System.out.println("[ChitChat] Using cloud server: " + DEFAULT_SERVER_URL);
                return DEFAULT_SERVER_URL;
            }
        } catch (Exception ignored) {}

        System.out.println("[ChitChat] Cloud unreachable — falling back to localhost:8080");
        return "http://localhost:8080";
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 440, 580);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("ChitChat");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMinWidth(440);
        stage.setMinHeight(580);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
