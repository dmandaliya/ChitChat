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

        return DEFAULT_SERVER_URL;
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
