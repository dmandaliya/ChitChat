package com.chitchat.client.controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ChatController implements Initializable {

    @FXML private Label userInfoLabel;
    @FXML private Label chatHeaderLabel;
    @FXML private ListView<String> userListView;
    @FXML private ListView<String> roomListView;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageInput;

    private final WebSocketService wsService = new WebSocketService();
    private final ApiService apiService = new ApiService(ChatClientApp.SERVER_URL);
    private String privateTarget = null;
    private String roomTarget = null;
    private final Map<String, String> roomDisplayToId = new HashMap<>();

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
                roomTarget = null;
                roomListView.getSelectionModel().clearSelection();
                chatHeaderLabel.setText("Private: @" + privateTarget);
            }
        });

        roomListView.setOnMouseClicked(e -> {
            String selected = roomListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            String roomId = roomDisplayToId.get(selected);
            if (roomId == null || roomId.isBlank()) return;

            if (roomId.equals(roomTarget)) {
                roomTarget = null;
                chatHeaderLabel.setText("Public Chat");
                roomListView.getSelectionModel().clearSelection();
            } else {
                roomTarget = roomId;
                privateTarget = null;
                userListView.getSelectionModel().clearSelection();
                chatHeaderLabel.setText("Room: #" + selected);
                wsService.subscribeToRoom(roomId);
                joinRoomIfNeeded(roomId);
            }
        });

        wsService.setOnMessage(msg -> Platform.runLater(() -> displayMessage(msg)));
        wsService.setOnUserList(csv -> Platform.runLater(() -> updateUserList(csv)));
        wsService.connect(ChatClientApp.SERVER_URL, session.getUsername());

        loadFriends();
        loadRooms();
    }

    @FXML
    private void handleSend() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;
        messageInput.clear();

        UserSession session = UserSession.getInstance();
        Message msg;
        if (roomTarget != null) {
            msg = new Message(MessageType.ROOM_MESSAGE, session.getUsername(), null, content);
            msg.setRoomId(roomTarget);
        } else if (privateTarget != null) {
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
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Scene scene = new Scene(loader.load(), 440, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage profileStage = new Stage();
            profileStage.setTitle("My Profile");
            profileStage.setScene(scene);
            profileStage.setResizable(false);
            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddFriend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Add a friend by username");
        dialog.setContentText("Username:");
        dialog.showAndWait().ifPresent(username -> {
            String target = username.trim();
            if (target.isEmpty()) {
                return;
            }
            String currentUser = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    apiService.sendFriendRequest(currentUser, target);
                    Platform.runLater(() -> {
                        showInfo("Friend request sent to @" + target);
                        loadFriends();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> showError("Failed to send request: " + e.getMessage()));
                }
            });
        });
    }

    @FXML
    private void handleCreateRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Group Room");
        dialog.setHeaderText("Create a group chat room");
        dialog.setContentText("Room name:");
        dialog.showAndWait().ifPresent(roomName -> {
            String trimmed = roomName.trim();
            if (trimmed.isEmpty()) {
                return;
            }

            String username = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> room = apiService.createRoom(trimmed, "", username);
                    Object id = room.get("id");
                    if (id != null) {
                        wsService.subscribeToRoom(id.toString());
                    }
                    Platform.runLater(() -> {
                        showInfo("Room created: " + trimmed);
                        loadRooms();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> showError("Failed to create room: " + e.getMessage()));
                }
            });
        });
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

    private void loadFriends() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> friends = apiService.getFriends(username);
                Platform.runLater(() -> {
                    String selected = userListView.getSelectionModel().getSelectedItem();
                    userListView.getItems().clear();
                    for (Map<String, Object> friend : friends) {
                        Object friendUsername = friend.get("username");
                        if (friendUsername != null) {
                            userListView.getItems().add(friendUsername.toString());
                        }
                    }
                    if (selected != null) {
                        userListView.getSelectionModel().select(selected);
                    }
                });
            } catch (IOException ignored) {
                // WS online list still provides a usable fallback list.
            }
        });
    }

    private void loadRooms() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> rooms = apiService.getRooms();
                Platform.runLater(() -> {
                    String selected = roomListView.getSelectionModel().getSelectedItem();
                    roomDisplayToId.clear();
                    roomListView.getItems().clear();

                    for (Map<String, Object> room : rooms) {
                        Object id = room.get("id");
                        Object name = room.get("name");
                        if (id == null || name == null) {
                            continue;
                        }

                        Object members = room.get("members");
                        boolean isMember = true;
                        if (members instanceof List<?> memberList) {
                            isMember = memberList.stream().anyMatch(m -> username.equals(String.valueOf(m)));
                        }
                        if (!isMember) {
                            continue;
                        }

                        String display = name.toString();
                        roomDisplayToId.put(display, id.toString());
                        roomListView.getItems().add(display);
                        wsService.subscribeToRoom(id.toString());
                    }

                    if (selected != null) {
                        roomListView.getSelectionModel().select(selected);
                    }
                });
            } catch (IOException ignored) {
                // Room list is optional for now and can be refreshed by user actions.
            }
        });
    }

    private void joinRoomIfNeeded(String roomId) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.joinRoom(Long.parseLong(roomId), username);
            } catch (Exception ignored) {
                // User may already be a member or room join may be unnecessary.
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Request failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("ChitChat");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
