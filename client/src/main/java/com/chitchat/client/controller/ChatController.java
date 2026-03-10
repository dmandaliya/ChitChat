package com.chitchat.client.controller;

import com.chitchat.client.ChatClientApp;
import com.chitchat.client.model.UserSession;
import com.chitchat.client.service.ApiService;
import com.chitchat.client.service.WebSocketService;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    @FXML private Label userInfoLabel;
    @FXML private Label chatHeaderLabel;
    @FXML private ListView<String> userListView;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageInput;

    private final WebSocketService wsService = new WebSocketService();
    private final ApiService apiService = new ApiService(ChatClientApp.SERVER_URL);
    private String privateTarget = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserSession session = UserSession.getInstance();
        userInfoLabel.setText(session.getDisplayName() + " (@" + session.getUsername() + ")");

        // Click on user in list to open private chat
        userListView.setOnMouseClicked(e -> {
            String selected = userListView.getSelectionModel().getSelectedItem();
            if (selected == null || selected.equals(session.getUsername())) return;

            if (selected.equals(privateTarget)) {
                privateTarget = null;
                chatHeaderLabel.setText("Public Chat");
                userListView.getSelectionModel().clearSelection();
            } else {
                privateTarget = selected;
                chatHeaderLabel.setText("Private: @" + privateTarget);
            }
        });

        wsService.setOnMessage(msg -> Platform.runLater(() -> displayMessage(msg)));
        wsService.setOnUserList(csv -> Platform.runLater(() -> updateUserList(csv)));
        wsService.connect(ChatClientApp.SERVER_URL, session.getUsername());
    }

    @FXML
    private void handleSend() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;
        messageInput.clear();

        UserSession session = UserSession.getInstance();
        Message msg;
        if (privateTarget != null) {
            msg = new Message(MessageType.PRIVATE_MESSAGE, session.getUsername(), privateTarget, content);
        } else {
            msg = new Message(MessageType.PUBLIC_MESSAGE, session.getUsername(), null, content);
        }
        wsService.sendMessage(msg);
    }

    private void displayMessage(Message msg) {
        UserSession session = UserSession.getInstance();
        boolean isMine = msg.getSender().equals(session.getUsername());
        boolean isSystem = "System".equals(msg.getSender());
        boolean isPrivate = msg.getType() == MessageType.PRIVATE_MESSAGE;

        HBox row = new HBox();
        row.setPadding(new Insets(2, 12, 2, 12));

        if (isSystem) {
            Label sysLabel = new Label(msg.getContent());
            sysLabel.getStyleClass().add("msg-system");
            row.setAlignment(Pos.CENTER);
            row.getChildren().add(sysLabel);
        } else {
            VBox bubble = new VBox(2);
            if (!isMine) {
                Label senderLabel = new Label(isPrivate
                        ? msg.getSender() + " \u2192 You (private)"
                        : msg.getSender());
                senderLabel.getStyleClass().add("msg-sender");
                bubble.getChildren().add(senderLabel);
            } else if (isPrivate) {
                Label senderLabel = new Label("You \u2192 " + msg.getReceiver() + " (private)");
                senderLabel.getStyleClass().add("msg-sender");
                bubble.getChildren().add(senderLabel);
            }

            Text text = new Text(msg.getContent());
            text.setWrappingWidth(340);
            Label content = new Label();
            content.setGraphic(text);
            content.getStyleClass().addAll("msg-bubble",
                    isMine ? "msg-mine" : "msg-theirs");
            if (isPrivate) content.getStyleClass().add("msg-private");

            bubble.getChildren().add(content);
            row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.getChildren().add(bubble);
        }

        messagesBox.getChildren().add(row);
        messagesScroll.setVvalue(1.0);
    }

    private void updateUserList(String csv) {
        userListView.getItems().clear();
        if (csv == null || csv.isBlank()) return;
        for (String u : csv.split(",")) {
            if (!u.isBlank()) userListView.getItems().add(u.trim());
        }
    }

    @FXML
    private void handleLogout() {
        UserSession session = UserSession.getInstance();
        wsService.disconnect(session.getUsername());
        try {
            apiService.logout(session.getUsername());
        } catch (IOException ignored) {}
        session.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 440, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) messageInput.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setTitle("ChitChat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
