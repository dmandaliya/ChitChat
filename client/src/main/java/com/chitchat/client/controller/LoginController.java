package com.chitchat.client.controller;

import com.chitchat.client.ChatClientApp;
import com.chitchat.client.model.UserSession;
import com.chitchat.client.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private TabPane tabPane;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    @FXML private TextField regFname;
    @FXML private TextField regLname;
    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private Label regError;

    private final ApiService api = new ApiService(ChatClientApp.SERVER_URL);

    @FXML
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();
        loginError.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Please fill in all fields.");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return api.login(username, password);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }).thenAcceptAsync(data -> openChat(data), Platform::runLater)
          .exceptionally(ex -> {
              Platform.runLater(() -> loginError.setText(ex.getCause().getMessage()));
              return null;
          });
    }

    @FXML
    private void handleRegister() {
        String fname    = regFname.getText().trim();
        String lname    = regLname.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        regError.setText("");

        if (fname.isEmpty() || lname.isEmpty() || username.isEmpty() || password.isEmpty()) {
            regError.setText("Please fill in all fields.");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return api.register(fname, lname, username, password);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }).thenAcceptAsync(data -> openChat(data), Platform::runLater)
          .exceptionally(ex -> {
              Platform.runLater(() -> regError.setText(ex.getCause().getMessage()));
              return null;
          });
    }

    private void openChat(Map<?, ?> userData) {
        UserSession session = UserSession.getInstance();
        session.setUsername((String) userData.get("username"));
        session.setFname((String) userData.get("fname"));
        session.setLname((String) userData.get("lname"));

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) loginUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setTitle("ChitChat — " + session.getDisplayName());
        } catch (IOException e) {
            loginError.setText("Failed to open chat: " + e.getMessage());
        }
    }
}
