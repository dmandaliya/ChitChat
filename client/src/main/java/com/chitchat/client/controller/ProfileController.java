package com.chitchat.client.controller;

import com.chitchat.client.ChatClientApp;
import com.chitchat.client.model.UserSession;
import com.chitchat.client.service.ApiService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitials;
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea bioArea;
    @FXML private Label profileMsg;

    private final ApiService apiService = new ApiService(ChatClientApp.SERVER_URL);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserSession session = UserSession.getInstance();

        // Set initials in avatar
        String display = session.getDisplayName();
        String initials = getInitials(display);
        avatarInitials.setText(initials);

        fullNameLabel.setText(display);
        usernameLabel.setText("@" + session.getUsername());

        statusCombo.setItems(FXCollections.observableArrayList("Online", "Away", "Busy", "Offline"));
        statusCombo.setValue("Online");

        loadProfile();
    }

    @FXML
    private void handleSave() {
        String username = UserSession.getInstance().getUsername();
        String status = statusCombo.getValue() != null ? statusCombo.getValue() : "Online";
        String bio = bioArea.getText() != null ? bioArea.getText().trim() : "";

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                apiService.updateProfile(username, status, bio);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            profileMsg.setText("Profile saved!");
            profileMsg.setStyle("-fx-text-fill: #0f766e;");
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String message = ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            profileMsg.setText("Save failed: " + message);
            profileMsg.setStyle("-fx-text-fill: #ff6b6b;");
        });

        new Thread(saveTask, "profile-save").start();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) bioArea.getScene().getWindow();
        stage.close();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private void loadProfile() {
        String username = UserSession.getInstance().getUsername();

        Task<Map<String, Object>> loadTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws IOException {
                return apiService.getProfile(username);
            }
        };

        loadTask.setOnSucceeded(e -> {
            Map<String, Object> profile = loadTask.getValue();
            if (profile == null) {
                return;
            }

            Object status = profile.get("status");
            Object bio = profile.get("bio");
            if (status != null && !status.toString().isBlank()) {
                statusCombo.setValue(status.toString());
            }
            if (bio != null) {
                bioArea.setText(bio.toString());
            }
        });

        loadTask.setOnFailed(e -> {
            profileMsg.setText("Could not load profile details.");
            profileMsg.setStyle("-fx-text-fill: #ff6b6b;");
        });

        new Thread(loadTask, "profile-load").start();
    }
}
