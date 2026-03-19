package com.chitchat.client.controller;

import com.chitchat.client.model.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitials;
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea bioArea;
    @FXML private Label profileMsg;

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
    }

    @FXML
    private void handleSave() {
        // TODO: Ayden — call PUT /api/users/{username}/profile with bio and status
        profileMsg.setText("Profile saved!");
        profileMsg.setStyle("-fx-text-fill: #ffb3c1;");
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
}
