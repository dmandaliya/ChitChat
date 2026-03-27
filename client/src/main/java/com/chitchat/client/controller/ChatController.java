package com.chitchat.client.controller;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.chitchat.shared.UserPreferences;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChatController implements Initializable {

    @FXML private HBox chatRoot;
    @FXML private Label userInfoLabel;
    @FXML private Label chatHeaderLabel;
    @FXML private ListView<String> userListView;
    @FXML private ListView<String> roomListView;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextField messageInput;
    @FXML private Label typingIndicatorLabel;
    @FXML private Button callVoiceButton;
    @FXML private Button callVideoButton;
    @FXML private Button leaveRoomButton;
    @FXML private Button endCallButton;
    @FXML private Button attachImageButton;

    private final WebSocketService wsService = new WebSocketService();
    private final ApiService apiService = new ApiService(ChatClientApp.SERVER_URL);
    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;
    private static final int MESSAGE_DEDUPE_WINDOW = 800;
    private static final long NOTIFICATION_COOLDOWN_MS = 1500;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final PauseTransition typingDebounce = new PauseTransition(Duration.millis(500));
    private final PauseTransition typingIndicatorTimeout = new PauseTransition(Duration.seconds(2.8));
    private final Timeline rosterRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(8), e -> {
                loadFriends();
                loadRooms();
            }));
    private final Map<String, Label> receiptStatusByMessageId = new HashMap<>();
    private final Map<String, List<String>> reactionsByMessageId = new HashMap<>();
    private final Map<String, String> friendDisplayToUsername = new HashMap<>();
    private final Map<String, Boolean> friendOnline = new HashMap<>();
    private final Map<String, Integer> friendUnread = new HashMap<>();
    private final Map<String, LocalDateTime> friendLastActivity = new HashMap<>();
    private final Map<String, String> friendLastPreview = new HashMap<>();
    private final Map<String, Long> friendTypingUntil = new HashMap<>();
    private final List<String> friendUsernames = new ArrayList<>();
    private final Map<String, String> roomIdToName = new HashMap<>();
    private final Map<String, Integer> roomUnread = new HashMap<>();
    private final Map<String, LocalDateTime> roomLastActivity = new HashMap<>();
    private final LinkedHashMap<String, Boolean> recentMessageKeys = new LinkedHashMap<>();
    private String privateTarget = null;
    private String roomTarget = null;
    private final Map<String, String> roomDisplayToId = new HashMap<>();
    private boolean loadingHistory = false;
    private String activeCallTarget = null;
    private UserPreferences activePreferences = new UserPreferences();
    private TrayIcon trayIcon;
    private boolean windowFocused = true;
    private long lastNotificationEpochMs = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserSession session = UserSession.getInstance();
        userInfoLabel.setText(session.getDisplayName() + " (@" + session.getUsername() + ")");
        initDesktopNotifications();
        Platform.runLater(this::bindWindowFocusTracking);

        // Click on user in list to open private chat
        userListView.setOnMouseClicked(e -> {
            String selectedDisplay = userListView.getSelectionModel().getSelectedItem();
            if (selectedDisplay == null) return;
            String selected = friendDisplayToUsername.get(selectedDisplay);
            if (selected == null || selected.equals(session.getUsername())) return;

            if (selected.equals(privateTarget)) {
                sendTypingStopped();
                privateTarget = null;
                updateChatHeader();
                userListView.getSelectionModel().clearSelection();
                clearTypingIndicator();
                loadPublicHistory();
                updateCallControls();
                updateRoomControls();
            } else {
                sendTypingStopped();
                privateTarget = selected;
                friendUnread.put(privateTarget, 0);
                roomTarget = null;
                roomListView.getSelectionModel().clearSelection();
                updateChatHeader();
                clearTypingIndicator();
                loadPrivateHistory(privateTarget);
                updateCallControls();
                updateRoomControls();
                renderFriendList();
            }
        });

        roomListView.setOnMouseClicked(e -> {
            String selected = roomListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            String roomId = roomDisplayToId.get(selected);
            if (roomId == null || roomId.isBlank()) return;

            if (roomId.equals(roomTarget)) {
                roomTarget = null;
                updateChatHeader();
                roomListView.getSelectionModel().clearSelection();
                clearTypingIndicator();
                loadPublicHistory();
                updateCallControls();
                updateRoomControls();
            } else {
                sendTypingStopped();
                roomTarget = roomId;
                roomUnread.put(roomTarget, 0);
                privateTarget = null;
                userListView.getSelectionModel().clearSelection();
                updateChatHeader();
                wsService.subscribeToRoom(roomId);
                joinRoomIfNeeded(roomId);
                clearTypingIndicator();
                loadRoomHistory(roomId);
                updateCallControls();
                updateRoomControls();
                renderRoomList();
            }
        });

        wsService.setOnMessage(msg -> Platform.runLater(() -> handleIncomingMessage(msg)));
        wsService.setOnUserList(csv -> Platform.runLater(() -> updateUserList(csv)));
        wsService.connect(ChatClientApp.SERVER_URL, session.getUsername());

        typingDebounce.setOnFinished(e -> sendTypingIndicator());
        typingIndicatorTimeout.setOnFinished(e -> clearTypingIndicator());
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (privateTarget == null) {
                return;
            }
            if (newVal == null || newVal.isBlank()) {
                typingDebounce.stop();
                sendTypingStopped();
                return;
            }
            typingDebounce.playFromStart();
        });

        loadFriends();
        loadRooms();
        loadPublicHistory();
        loadAndApplyPreferences();

        rosterRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        rosterRefreshTimeline.play();
    }

    @FXML
    private void handleSend() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;
        sendTypingStopped();
        messageInput.clear();
        clearTypingIndicator();

        sendContent(content);
    }

    @FXML
    private void handleSendImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        Stage stage = (Stage) messageInput.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String payload = createImagePayload(file);
                Platform.runLater(() -> sendContent(payload));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to attach image: " + e.getMessage()));
            }
        });
    }

    private void sendContent(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

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

    private void handleIncomingMessage(Message msg) {
        if (msg == null || msg.getType() == null) {
            return;
        }

        if (msg.getType() == MessageType.TYPING) {
            handleTypingSignal(msg);
            return;
        }

        if (msg.getType() == MessageType.READ_RECEIPT) {
            handleReadReceipt(msg);
            return;
        }

        if (msg.getType() == MessageType.CALL_OFFER
                || msg.getType() == MessageType.CALL_ANSWER
                || msg.getType() == MessageType.CALL_END
                || msg.getType() == MessageType.CALL_REJECT) {
            handleCallSignal(msg);
            return;
        }

        if (!acceptMessageIfNew(msg)) {
            return;
        }

        maybeClearTypingIndicatorOnIncoming(msg);

        trackMessageActivity(msg);

        boolean willDisplay = shouldDisplayMessage(msg);
        maybeNotifyIncomingMessage(msg, willDisplay);

        if (!willDisplay) {
            return;
        }

        displayMessage(msg, false);
    }

    private void displayMessage(Message msg, boolean fromHistory) {
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

            Label content = new Label();
            content.getStyleClass().addAll("msg-bubble",
                    isMine ? "msg-mine" : "msg-theirs");
            if (isPrivate) content.getStyleClass().add("msg-private");

            Node messageNode;
            if (isImagePayload(msg.getContent())) {
                messageNode = buildImageNode(msg.getContent());
            } else {
                Text text = new Text(msg.getContent());
                text.setWrappingWidth(340);
                messageNode = text;
            }
            content.setGraphic(messageNode);

            attachReactionMenu(content, bubble, msg);

            bubble.getChildren().add(content);

            HBox meta = new HBox(6);
            meta.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            Label time = new Label(formatTime(msg.getTimestamp()));
            time.getStyleClass().add("msg-time");
            meta.getChildren().add(time);

            if (isPrivate && isMine && msg.getId() != null && !msg.getId().isBlank()) {
                if (activePreferences.isShowReadReceipts()) {
                    Label status = receiptStatusByMessageId.computeIfAbsent(msg.getId(), k -> {
                        Label l = new Label("Sent");
                        l.getStyleClass().add("msg-status");
                        return l;
                    });
                    meta.getChildren().add(status);
                }
            }

            bubble.getChildren().add(meta);
            row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.getChildren().add(bubble);

            if (!fromHistory && !isMine && isPrivate && msg.getId() != null && !msg.getId().isBlank()) {
                sendReadReceipt(msg.getId());
            }

            addReactionBadgeIfPresent(bubble, msg.getId());
        }

        messagesBox.getChildren().add(row);
        // Auto-scroll to latest message only for new messages (not history)
        if (!fromHistory) {
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> Platform.runLater(() -> messagesScroll.setVvalue(1.0)));
            delay.play();
        }
    }

    private boolean shouldDisplayMessage(Message msg) {
        MessageType type = msg.getType();
        if (type == MessageType.PUBLIC_MESSAGE) {
            return privateTarget == null && roomTarget == null;
        }
        if (type == MessageType.PRIVATE_MESSAGE) {
            if (privateTarget == null) {
                return false;
            }
            String me = UserSession.getInstance().getUsername();
            boolean betweenActiveUsers =
                    (me.equals(msg.getSender()) && privateTarget.equals(msg.getReceiver()))
                            || (privateTarget.equals(msg.getSender()) && me.equals(msg.getReceiver()));
            return betweenActiveUsers;
        }
        if (type == MessageType.ROOM_MESSAGE) {
            return roomTarget != null && roomTarget.equals(msg.getRoomId());
        }
        return false;
    }

    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(timeFmt);
    }

    private void handleReadReceipt(Message msg) {
        if (!activePreferences.isShowReadReceipts()) {
            return;
        }
        if (msg.getContent() == null || msg.getContent().isBlank()) {
            return;
        }
        Label status = receiptStatusByMessageId.get(msg.getContent());
        if (status != null) {
            status.setText("Seen");
        }
    }

    private void handleTypingSignal(Message msg) {
        if (privateTarget == null || msg.getSender() == null || !msg.getSender().equals(privateTarget)) {
            return;
        }

        String typingState = msg.getContent() != null ? msg.getContent().trim().toLowerCase() : "typing";
        if ("stopped".equals(typingState)) {
            friendTypingUntil.remove(msg.getSender());
            renderFriendList();
            clearTypingIndicator();
            return;
        }

        friendTypingUntil.put(msg.getSender(), System.currentTimeMillis() + 3000);
        renderFriendList();
        typingIndicatorLabel.setManaged(true);
        typingIndicatorLabel.setVisible(true);
        typingIndicatorLabel.setText(privateTarget + " is typing...");
        typingIndicatorTimeout.playFromStart();
    }

    private void clearTypingIndicator() {
        typingIndicatorLabel.setText("");
        typingIndicatorLabel.setManaged(false);
        typingIndicatorLabel.setVisible(false);
    }

    private void sendTypingIndicator() {
        sendTypingSignal("typing");
    }

    private void sendTypingStopped() {
        sendTypingSignal("stopped");
    }

    private void sendTypingSignal(String state) {
        if (privateTarget == null) {
            return;
        }
        String sender = UserSession.getInstance().getUsername();
        Message typing = new Message(MessageType.TYPING, sender, privateTarget, state);
        wsService.sendMessage(typing);
    }

    private void maybeClearTypingIndicatorOnIncoming(Message msg) {
        if (msg.getType() != MessageType.PRIVATE_MESSAGE || privateTarget == null) {
            return;
        }

        String me = UserSession.getInstance().getUsername();
        boolean fromActiveUser = privateTarget.equals(msg.getSender()) && me.equals(msg.getReceiver());
        if (fromActiveUser) {
            friendTypingUntil.remove(msg.getSender());
            renderFriendList();
            clearTypingIndicator();
        }
    }

    private void initDesktopNotifications() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage trayImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            trayIcon = new TrayIcon(trayImage, "ChitChat");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (AWTException | SecurityException ignored) {
            trayIcon = null;
        }
    }

    private void bindWindowFocusTracking() {
        if (chatRoot == null || chatRoot.getScene() == null) {
            return;
        }
        Stage stage = (Stage) chatRoot.getScene().getWindow();
        if (stage == null) {
            return;
        }
        windowFocused = stage.isFocused();
        stage.focusedProperty().addListener((obs, oldVal, isFocused) -> windowFocused = isFocused);
    }

    private void maybeNotifyIncomingMessage(Message msg, boolean willDisplay) {
        if (!activePreferences.isNotis() || msg == null || msg.getType() == null) {
            return;
        }

        if (msg.getType() != MessageType.PUBLIC_MESSAGE
                && msg.getType() != MessageType.PRIVATE_MESSAGE
                && msg.getType() != MessageType.ROOM_MESSAGE) {
            return;
        }

        String me = UserSession.getInstance().getUsername();
        if (me.equals(msg.getSender())) {
            return;
        }

        if (windowFocused && willDisplay) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastNotificationEpochMs < NOTIFICATION_COOLDOWN_MS) {
            return;
        }
        lastNotificationEpochMs = now;

        String title;
        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            title = "Private message from @" + msg.getSender();
        } else if (msg.getType() == MessageType.ROOM_MESSAGE) {
            String roomName = getRoomDisplayName(msg.getRoomId());
            title = "Room #" + roomName;
        } else {
            title = "Public message from @" + msg.getSender();
        }

        String content = msg.getContent() != null ? msg.getContent() : "";
        String base = isImagePayload(content) ? "Photo" : content;
        String preview = base.length() > 120 ? base.substring(0, 120) + "..." : base;
        if (trayIcon != null) {
            trayIcon.displayMessage(title, preview, TrayIcon.MessageType.NONE);
        }
    }

    private void sendReadReceipt(String messageId) {
        if (!activePreferences.isShowReadReceipts()) {
            return;
        }
        if (loadingHistory || messageId == null || messageId.isBlank()) {
            return;
        }
        String sender = UserSession.getInstance().getUsername();
        Message receipt = new Message(MessageType.READ_RECEIPT, sender, null, messageId);
        receipt.setId(messageId);
        wsService.sendMessage(receipt);
    }

    @FXML
    private void handleVoiceCall() {
        startCall(false);
    }

    @FXML
    private void handleVideoCall() {
        startCall(true);
    }

    @FXML
    private void handleEndCall() {
        endCall("Call ended");
    }

    private void startCall(boolean withVideo) {
        if (privateTarget == null || privateTarget.isBlank()) {
            return;
        }
        if (activeCallTarget != null) {
            showInfo("You are already in a call with @" + activeCallTarget);
            return;
        }

        String sender = UserSession.getInstance().getUsername();
        Message offer = new Message(MessageType.CALL_OFFER, sender, privateTarget, withVideo ? "VIDEO" : "VOICE");
        wsService.sendMessage(offer);
        activeCallTarget = privateTarget;
        updateCallControls();
        showInfo((withVideo ? "Video" : "Voice") + " call offer sent to @" + privateTarget);
    }

    private void endCall(String statusMessage) {
        if (activeCallTarget == null || activeCallTarget.isBlank()) {
            return;
        }

        String sender = UserSession.getInstance().getUsername();
        Message end = new Message(MessageType.CALL_END, sender, activeCallTarget, "");
        wsService.sendMessage(end);
        activeCallTarget = null;
        updateCallControls();
        showInfo(statusMessage);
    }

    private void handleCallSignal(Message msg) {
        String me = UserSession.getInstance().getUsername();
        if (msg.getReceiver() != null && !me.equals(msg.getReceiver())) {
            return;
        }

        if (msg.getType() == MessageType.CALL_OFFER) {
            Alert incoming = new Alert(Alert.AlertType.CONFIRMATION);
            incoming.setTitle("Incoming Call");
            incoming.setHeaderText("Incoming " + ("VIDEO".equalsIgnoreCase(msg.getContent()) ? "video" : "voice")
                    + " call from @" + msg.getSender());
            incoming.setContentText("Accept this call?");
            ButtonType accept = new ButtonType("Accept");
            ButtonType reject = new ButtonType("Reject");
            incoming.getButtonTypes().setAll(accept, reject);

            incoming.showAndWait().ifPresent(choice -> {
                if (choice == accept) {
                    Message answer = new Message(MessageType.CALL_ANSWER, me, msg.getSender(), "accepted");
                    wsService.sendMessage(answer);
                    activeCallTarget = msg.getSender();
                    updateCallControls();
                    showInfo("Call connected with @" + activeCallTarget);
                } else {
                    Message rejectMsg = new Message(MessageType.CALL_REJECT, me, msg.getSender(), "rejected");
                    wsService.sendMessage(rejectMsg);
                }
            });
            return;
        }

        if (msg.getType() == MessageType.CALL_ANSWER) {
            activeCallTarget = msg.getSender();
            updateCallControls();
            showInfo("@" + msg.getSender() + " accepted your call");
            return;
        }

        if (msg.getType() == MessageType.CALL_REJECT) {
            if (msg.getSender() != null && msg.getSender().equals(activeCallTarget)) {
                activeCallTarget = null;
                updateCallControls();
            }
            showInfo("@" + msg.getSender() + " rejected the call");
            return;
        }

        if (msg.getType() == MessageType.CALL_END) {
            if (msg.getSender() != null && msg.getSender().equals(activeCallTarget)) {
                activeCallTarget = null;
                updateCallControls();
            }
            showInfo("@" + msg.getSender() + " ended the call");
        }
    }

    private void updateCallControls() {
        boolean privateChat = privateTarget != null && !privateTarget.isBlank();
        boolean inCall = activeCallTarget != null && !activeCallTarget.isBlank();

        callVoiceButton.setManaged(privateChat && !inCall);
        callVoiceButton.setVisible(privateChat && !inCall);

        callVideoButton.setManaged(privateChat && !inCall);
        callVideoButton.setVisible(privateChat && !inCall);

        endCallButton.setManaged(inCall);
        endCallButton.setVisible(inCall);
    }

    private void updateRoomControls() {
        boolean inRoom = roomTarget != null && !roomTarget.isBlank();
        leaveRoomButton.setManaged(inRoom);
        leaveRoomButton.setVisible(inRoom);
    }

    private boolean isImagePayload(String content) {
        return content != null && content.startsWith("data:image/") && content.contains(";base64,");
    }

    private Node buildImageNode(String payload) {
        try {
            int comma = payload.indexOf(',');
            if (comma < 0) {
                return new Text("[Invalid image payload]");
            }
            String base64 = payload.substring(comma + 1);
            byte[] data = Base64.getDecoder().decode(base64);
            Image image = new Image(new ByteArrayInputStream(data));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(220);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("msg-image");
            return imageView;
        } catch (Exception ex) {
            return new Text("[Unable to render image]");
        }
    }

    private String createImagePayload(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IOException("Image must be 2MB or smaller.");
        }

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null || !mimeType.startsWith("image/")) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png")) mimeType = "image/png";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (name.endsWith(".gif")) mimeType = "image/gif";
            else if (name.endsWith(".webp")) mimeType = "image/webp";
            else mimeType = "image/png";
        }

        String encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeType + ";base64," + encoded;
    }

    private void updateUserList(String csv) {
        friendOnline.clear();
        if (csv != null && !csv.isBlank()) {
            for (String u : csv.split(",")) {
                if (!u.isBlank()) {
                    friendOnline.put(u.trim(), true);
                }
            }
        }
        renderFriendList();
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
        showAddFriendDialog();
    }

    @FXML
    private void handleCreateRoom() {
        showCreateRoomDialog();
    }

    @FXML
    private void handleLeaveRoom() {
        if (roomTarget == null || roomTarget.isBlank()) {
            return;
        }

        String roomId = roomTarget;
        String username = UserSession.getInstance().getUsername();
        String roomName = roomIdToName.getOrDefault(roomId, roomId);

        CompletableFuture.runAsync(() -> {
            try {
                apiService.leaveRoom(Long.parseLong(roomId), username);
                Platform.runLater(() -> {
                    roomTarget = null;
                    updateChatHeader();
                    clearTypingIndicator();
                    loadPublicHistory();
                    loadRooms();
                    updateCallControls();
                    updateRoomControls();
                    showInfo("Left room #" + roomName);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to leave room: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleFriendRequests() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> pending = apiService.getPendingFriendRequests(username);
                Platform.runLater(() -> showFriendRequestsDialog(pending));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to load requests: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleLogout() {
        rosterRefreshTimeline.stop();

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
                    String activePrivate = privateTarget;
                    friendUsernames.clear();
                    for (Map<String, Object> friend : friends) {
                        Object friendUsername = friend.get("username");
                        if (friendUsername != null) {
                            ensureFriendTracked(friendUsername.toString());
                        }
                    }

                    if (activePrivate != null && !activePrivate.isBlank()) {
                        ensureFriendTracked(activePrivate);
                    }

                    renderFriendList();
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
                    roomIdToName.clear();

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

                        String roomId = id.toString();
                        roomIdToName.put(roomId, name.toString());
                        roomUnread.putIfAbsent(roomId, 0);
                        roomLastActivity.putIfAbsent(roomId, LocalDateTime.MIN);
                        wsService.subscribeToRoom(roomId);
                    }
                    renderRoomList();
                    updateChatHeader();
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

    private void loadPublicHistory() {
        clearMessages();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getPublicMessages();
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {
                // Live stream still works if history endpoint is unavailable.
            }
        });
    }

    private void loadPrivateHistory(String otherUser) {
        clearMessages();
        String me = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getPrivateMessages(me, otherUser);
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {
                // Live stream still works if history endpoint is unavailable.
            }
        });
    }

    private void loadRoomHistory(String roomId) {
        clearMessages();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getRoomMessages(roomId);
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {
                // Live stream still works if history endpoint is unavailable.
            }
        });
    }

    private String getRoomDisplayName(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return "";
        }
        return roomIdToName.getOrDefault(roomId, roomId);
    }

    private void updateChatHeader() {
        if (roomTarget != null) {
            chatHeaderLabel.setText("Room: #" + getRoomDisplayName(roomTarget));
            return;
        }
        if (privateTarget != null) {
            chatHeaderLabel.setText("Private: @" + privateTarget);
            return;
        }
        chatHeaderLabel.setText("Public Chat");
    }

    private void renderHistory(List<Map<String, Object>> rawHistory) {
        loadingHistory = true;
        try {
            if (rawHistory == null) {
                return;
            }
            for (Map<String, Object> raw : rawHistory) {
                Message msg = toMessage(raw);
                cacheHistoryReactions(raw, msg != null ? msg.getId() : null);
                if (msg != null && shouldDisplayMessage(msg)) {
                    displayMessage(msg, true);
                }
            }
            // Scroll to bottom after all history is loaded
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> Platform.runLater(() -> messagesScroll.setVvalue(1.0)));
            delay.play();
        } finally {
            loadingHistory = false;
        }
    }

    private Message toMessage(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }
        try {
            Message msg = new Message();
            msg.setId(stringVal(raw.get("id")));
            msg.setSender(stringVal(raw.get("sender")));
            msg.setReceiver(stringVal(raw.get("receiver")));
            msg.setContent(stringVal(raw.get("content")));
            msg.setRoomId(stringVal(raw.get("roomId")));

            String typeRaw = stringVal(raw.get("type"));
            if (typeRaw != null && !typeRaw.isBlank()) {
                msg.setType(MessageType.valueOf(typeRaw));
            }

            String tsRaw = stringVal(raw.get("timestamp"));
            if (tsRaw != null && !tsRaw.isBlank()) {
                msg.setTimestamp(LocalDateTime.parse(tsRaw));
            }
            return msg;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stringVal(Object value) {
        return value == null ? null : value.toString();
    }

    private void clearMessages() {
        messagesBox.getChildren().clear();
        receiptStatusByMessageId.clear();
        reactionsByMessageId.clear();
        synchronized (recentMessageKeys) {
            recentMessageKeys.clear();
        }
    }

    private boolean acceptMessageIfNew(Message msg) {
        MessageType type = msg.getType();
        if (type != MessageType.PUBLIC_MESSAGE
                && type != MessageType.PRIVATE_MESSAGE
                && type != MessageType.ROOM_MESSAGE) {
            return true;
        }

        String dedupeKey = buildMessageDedupeKey(msg);
        synchronized (recentMessageKeys) {
            if (recentMessageKeys.containsKey(dedupeKey)) {
                return false;
            }
            recentMessageKeys.put(dedupeKey, Boolean.TRUE);
            if (recentMessageKeys.size() > MESSAGE_DEDUPE_WINDOW) {
                String oldest = recentMessageKeys.keySet().iterator().next();
                recentMessageKeys.remove(oldest);
            }
        }
        return true;
    }

    private String buildMessageDedupeKey(Message msg) {
        if (msg.getId() != null && !msg.getId().isBlank()) {
            return "id:" + msg.getId();
        }

        String type = msg.getType() != null ? msg.getType().name() : "";
        String sender = msg.getSender() != null ? msg.getSender() : "";
        String receiver = msg.getReceiver() != null ? msg.getReceiver() : "";
        String roomId = msg.getRoomId() != null ? msg.getRoomId() : "";
        String content = msg.getContent() != null ? msg.getContent() : "";
        String timestamp = msg.getTimestamp() != null ? msg.getTimestamp().toString() : "";
        return type + "|" + sender + "|" + receiver + "|" + roomId + "|" + timestamp + "|" + content;
    }

    private String toPreviewText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String preview = isImagePayload(content) ? "Photo" : content.trim().replaceAll("\\s+", " ");
        return preview.length() > 26 ? preview.substring(0, 26) + "..." : preview;
    }

    private void attachReactionMenu(Label contentLabel, VBox bubble, Message msg) {
        if (msg == null || msg.getId() == null || msg.getId().isBlank()) {
            return;
        }

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(createReactionMenuItem("❤️", msg.getId(), bubble));
        menu.getItems().add(createReactionMenuItem("😂", msg.getId(), bubble));
        menu.getItems().add(createReactionMenuItem("🔥", msg.getId(), bubble));
        menu.getItems().add(createReactionMenuItem("👍", msg.getId(), bubble));
        contentLabel.setContextMenu(menu);
    }

    private MenuItem createReactionMenuItem(String emoji, String messageId, VBox bubble) {
        MenuItem item = new MenuItem("React " + emoji);
        item.setOnAction(e -> addReaction(messageId, emoji, bubble));
        return item;
    }

    private void addReaction(String messageId, String emoji, VBox bubble) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.addReaction(messageId, username, emoji);
                Platform.runLater(() -> {
                    reactionsByMessageId.computeIfAbsent(messageId, k -> new ArrayList<>()).add(emoji);
                    upsertReactionLabel(bubble, messageId);
                });
            } catch (IOException ex) {
                Platform.runLater(() -> showError("Failed to react: " + ex.getMessage()));
            }
        });
    }

    private void addReactionBadgeIfPresent(VBox bubble, String messageId) {
        if (messageId == null || messageId.isBlank() || !reactionsByMessageId.containsKey(messageId)) {
            return;
        }
        upsertReactionLabel(bubble, messageId);
    }

    private void upsertReactionLabel(VBox bubble, String messageId) {
        List<String> reactions = reactionsByMessageId.get(messageId);
        if (reactions == null || reactions.isEmpty()) {
            return;
        }

        Label reactionLabel = null;
        for (int i = 0; i < bubble.getChildren().size(); i++) {
            if (bubble.getChildren().get(i) instanceof Label l && l.getStyleClass().contains("msg-reactions")) {
                reactionLabel = l;
                break;
            }
        }
        if (reactionLabel == null) {
            reactionLabel = new Label();
            reactionLabel.getStyleClass().add("msg-reactions");
            bubble.getChildren().add(reactionLabel);
        }
        reactionLabel.setText(String.join(" ", reactions));
    }

    private void cacheHistoryReactions(Map<String, Object> raw, String messageId) {
        if (messageId == null || messageId.isBlank() || raw == null) {
            return;
        }
        Object value = raw.get("reactions");
        if (!(value instanceof List<?> list)) {
            return;
        }

        List<String> emojis = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> reactionMap) {
                Object emoji = reactionMap.get("emoji");
                if (emoji != null && !emoji.toString().isBlank()) {
                    emojis.add(emoji.toString());
                }
            }
        }
        if (!emojis.isEmpty()) {
            reactionsByMessageId.put(messageId, emojis);
        }
    }

    private void showError(String message) {
        String safeMessage = (message == null || message.isBlank())
                ? "An unexpected error occurred."
                : message;
        showStatusDialog("Request failed", safeMessage, true);
    }

    private void showInfo(String message) {
        String safeMessage = (message == null || message.isBlank())
                ? "Action completed successfully."
                : message;
        showStatusDialog("Notice", safeMessage, false);
    }

    private void showStatusDialog(String header, String message, boolean errorStyle) {
        Runnable uiTask = () -> {
            Stage dialog = new Stage();
            dialog.setTitle("ChitChat");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            if (chatRoot != null && chatRoot.getScene() != null) {
                dialog.initOwner(chatRoot.getScene().getWindow());
            }

            Text headerLabel = new Text(header);
            headerLabel.getStyleClass().add("status-dialog-header");

            Text messageLabel = new Text(message);
            messageLabel.getStyleClass().add("status-dialog-message");
            messageLabel.setWrappingWidth(380);

            Button okButton = new Button("OK");
            okButton.setDefaultButton(true);
            okButton.setOnAction(e -> dialog.close());
            okButton.getStyleClass().add("status-dialog-ok");
            if (errorStyle) {
                okButton.getStyleClass().add("status-dialog-ok-error");
            }

            HBox actions = new HBox(okButton);
            actions.setAlignment(Pos.CENTER_RIGHT);

            VBox root = new VBox(12, headerLabel, messageLabel, actions);
            root.setPadding(new Insets(16));
            root.getStyleClass().add("status-dialog-root");
            if (chatRoot != null) {
                for (String className : chatRoot.getStyleClass()) {
                    if (className.startsWith("pref-")) {
                        root.getStyleClass().add(className);
                    }
                }
            }

            Scene scene = new Scene(root, 420, Region.USE_COMPUTED_SIZE);
            URL css = getClass().getResource("/css/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            dialog.setScene(scene);
            dialog.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            Platform.runLater(uiTask);
        }
    }

    private void showFriendRequestsDialog(List<Map<String, Object>> pending) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Friend Requests");
        dialog.setHeaderText("Pending requests");

        ListView<String> list = new ListView<>();
        list.setPrefHeight(220);
        if (pending != null) {
            for (Map<String, Object> item : pending) {
                Object username = item.get("username");
                if (username != null) {
                    list.getItems().add(username.toString());
                }
            }
        }

        Label hint = new Label(list.getItems().isEmpty()
                ? "No pending friend requests."
                : "Select a request and choose Accept or Reject.");

        VBox content = new VBox(10, hint, list);
        dialog.getDialogPane().setContent(content);

        ButtonType acceptType = new ButtonType("Accept");
        ButtonType rejectType = new ButtonType("Reject");
        ButtonType closeType = ButtonType.CLOSE;
        dialog.getDialogPane().getButtonTypes().addAll(acceptType, rejectType, closeType);

        Button acceptBtn = (Button) dialog.getDialogPane().lookupButton(acceptType);
        Button rejectBtn = (Button) dialog.getDialogPane().lookupButton(rejectType);
        acceptBtn.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        rejectBtn.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

        acceptBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String requester = list.getSelectionModel().getSelectedItem();
            if (requester == null) {
                return;
            }
            processFriendRequestAction(requester, true, list, hint);
        });

        rejectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String requester = list.getSelectionModel().getSelectedItem();
            if (requester == null) {
                return;
            }
            processFriendRequestAction(requester, false, list, hint);
        });

        dialog.showAndWait();
    }

    private void showAddFriendDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Friends");
        dialog.setHeaderText("Send requests or remove existing friends");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by username or name");

        ListView<String> resultsList = new ListView<>();
        resultsList.setPrefHeight(180);

        Label hint = new Label("Type at least 2 characters to search.");

        ListView<String> currentFriendsList = new ListView<>();
        currentFriendsList.setPrefHeight(150);
        List<String> currentFriends = new ArrayList<>(friendUsernames);
        currentFriends.sort(String::compareToIgnoreCase);
        currentFriendsList.getItems().addAll(currentFriends);

        Label removeHint = new Label(currentFriends.isEmpty()
                ? "No friends to remove yet."
                : "Select a friend and click Remove Friend.");

        VBox content = new VBox(10,
                new Label("Add / Request Friends"),
                searchField,
                hint,
                resultsList,
                new Separator(),
                new Label("Current Friends"),
                removeHint,
                currentFriendsList);
        dialog.getDialogPane().setContent(content);

        ButtonType searchType = new ButtonType("Search");
        ButtonType sendType = new ButtonType("Send Request");
        ButtonType removeType = new ButtonType("Remove Friend");
        dialog.getDialogPane().getButtonTypes().addAll(searchType, sendType, removeType, ButtonType.CLOSE);

        Button searchBtn = (Button) dialog.getDialogPane().lookupButton(searchType);
        Button sendBtn = (Button) dialog.getDialogPane().lookupButton(sendType);
        Button removeBtn = (Button) dialog.getDialogPane().lookupButton(removeType);

        searchBtn.disableProperty().bind(searchField.textProperty().length().lessThan(2));
        sendBtn.disableProperty().bind(resultsList.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.disableProperty().bind(currentFriendsList.getSelectionModel().selectedItemProperty().isNull());

        searchBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String query = searchField.getText() != null ? searchField.getText().trim() : "";
            if (query.length() < 2) {
                hint.setText("Type at least 2 characters to search.");
                return;
            }
            performUserSearch(query, resultsList, hint);
        });

        sendBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            String username = selected.split("\\s+")[0];
            sendFriendRequestToTarget(username, dialog);
        });

        removeBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String selectedFriend = currentFriendsList.getSelectionModel().getSelectedItem();
            if (selectedFriend == null || selectedFriend.isBlank()) {
                return;
            }
            removeFriendTarget(selectedFriend, currentFriendsList, removeHint);
        });

        dialog.showAndWait();
    }

    private void showCreateRoomDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Group Room");
        dialog.setHeaderText("Create a room and invite members");

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Room name");

        ListView<String> membersList = new ListView<>();
        membersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        membersList.setPrefHeight(200);
        membersList.getItems().addAll(friendUsernames);

        Label hint = new Label(friendUsernames.isEmpty()
                ? "No friends available to invite yet."
                : "Optional: select friends to invite.");

        VBox content = new VBox(10,
                new Label("Room Name"),
                roomNameField,
                new Label("Invite Members"),
                membersList,
                hint);
        dialog.getDialogPane().setContent(content);

        ButtonType createType = new ButtonType("Create");
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createType);
        createBtn.disableProperty().bind(roomNameField.textProperty().isEmpty());

        createBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String roomName = roomNameField.getText() != null ? roomNameField.getText().trim() : "";
            if (roomName.isBlank()) {
                hint.setText("Room name is required.");
                return;
            }

            List<String> invited = new ArrayList<>(membersList.getSelectionModel().getSelectedItems());
            createRoomWithInvites(roomName, invited, dialog);
        });

        dialog.showAndWait();
    }

    private void createRoomWithInvites(String roomName, List<String> invited, Dialog<ButtonType> dialog) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> room = apiService.createRoom(roomName, "", username);
                Object idRaw = room.get("id");
                if (idRaw == null) {
                    throw new IOException("Room creation response missing id.");
                }

                long roomId = Long.parseLong(idRaw.toString());
                String roomIdText = Long.toString(roomId);
                for (String member : invited) {
                    if (member == null || member.isBlank() || username.equals(member)) {
                        continue;
                    }
                    try {
                        apiService.joinRoom(roomId, member);
                    } catch (IOException ignored) {
                        // Continue inviting other users; server can reject duplicates.
                    }
                }

                Platform.runLater(() -> {
                    wsService.subscribeToRoom(roomIdText);
                    roomTarget = roomIdText;
                    roomUnread.put(roomIdText, 0);
                    privateTarget = null;
                    updateChatHeader();
                    dialog.close();
                    loadRooms();
                    loadRoomHistory(roomIdText);
                    updateCallControls();
                    updateRoomControls();
                    showInfo("Room created: " + roomName);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Failed to create room: " + ex.getMessage()));
            }
        });
    }

    private void performUserSearch(String query, ListView<String> resultsList, Label hint) {
        String currentUser = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> results = apiService.searchUsers(query);
                Platform.runLater(() -> {
                    resultsList.getItems().clear();
                    for (Map<String, Object> user : results) {
                        Object username = user.get("username");
                        if (username == null || currentUser.equals(username.toString())) {
                            continue;
                        }
                        String fname = user.get("fname") != null ? user.get("fname").toString() : "";
                        String lname = user.get("lname") != null ? user.get("lname").toString() : "";
                        String fullName = (fname + " " + lname).trim();
                        if (fullName.isBlank()) {
                            resultsList.getItems().add(username.toString());
                        } else {
                            resultsList.getItems().add(username + "    " + fullName);
                        }
                    }

                    if (resultsList.getItems().isEmpty()) {
                        hint.setText("No users found.");
                    } else {
                        hint.setText("Select a user and send a request.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> hint.setText("Search failed: " + e.getMessage()));
            }
        });
    }

    private void sendFriendRequestToTarget(String targetUsername, Dialog<ButtonType> dialog) {
        String currentUser = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.sendFriendRequest(currentUser, targetUsername);
                Platform.runLater(() -> {
                    dialog.close();
                    showInfo("Friend request sent to @" + targetUsername);
                    loadFriends();
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to send request: " + e.getMessage()));
            }
        });
    }

    private void removeFriendTarget(String targetUsername, ListView<String> currentFriendsList, Label removeHint) {
        String currentUser = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.removeFriend(currentUser, targetUsername);
                Platform.runLater(() -> {
                    currentFriendsList.getItems().remove(targetUsername);

                    friendUsernames.remove(targetUsername);
                    friendUnread.remove(targetUsername);
                    friendLastActivity.remove(targetUsername);
                    friendLastPreview.remove(targetUsername);
                    friendTypingUntil.remove(targetUsername);

                    if (targetUsername.equals(privateTarget)) {
                        privateTarget = null;
                        userListView.getSelectionModel().clearSelection();
                        clearTypingIndicator();
                        updateChatHeader();
                        loadPublicHistory();
                        updateCallControls();
                        updateRoomControls();
                    }

                    renderFriendList();
                    if (currentFriendsList.getItems().isEmpty()) {
                        removeHint.setText("No friends to remove yet.");
                    }

                    showInfo("Removed @" + targetUsername + " from friends");
                    loadFriends();
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to remove friend: " + e.getMessage()));
            }
        });
    }

    private void trackMessageActivity(Message msg) {
        if (msg == null || msg.getType() == null) {
            return;
        }

        String me = UserSession.getInstance().getUsername();
        LocalDateTime activityTime = msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now();

        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            String other = me.equals(msg.getSender()) ? msg.getReceiver() : msg.getSender();
            if (other == null || other.isBlank()) {
                return;
            }

            ensureFriendTracked(other);

            friendLastActivity.put(other, activityTime);
            friendLastPreview.put(other, toPreviewText(msg.getContent()));
            if (!me.equals(msg.getSender()) && (privateTarget == null || !privateTarget.equals(other))) {
                friendUnread.put(other, friendUnread.getOrDefault(other, 0) + 1);
            }
            renderFriendList();
            return;
        }

        if (msg.getType() == MessageType.ROOM_MESSAGE) {
            String roomId = msg.getRoomId();
            if (roomId == null || roomId.isBlank()) {
                return;
            }

            roomLastActivity.put(roomId, activityTime);
            if (!me.equals(msg.getSender()) && (roomTarget == null || !roomTarget.equals(roomId))) {
                roomUnread.put(roomId, roomUnread.getOrDefault(roomId, 0) + 1);
            }
            renderRoomList();
        }
    }

    private void renderFriendList() {
        String selectedUser = privateTarget;
        friendDisplayToUsername.clear();
        userListView.getItems().clear();

        List<String> ordered = new ArrayList<>(friendUsernames);
        ordered.sort(Comparator
                .comparing((String u) -> friendOnline.getOrDefault(u, false)).reversed()
                .thenComparing((String u) -> friendLastActivity.getOrDefault(u, LocalDateTime.MIN)).reversed()
            .thenComparing(u -> u.toLowerCase()));

        String displayToSelect = null;
        long now = System.currentTimeMillis();
        for (String username : ordered) {
            int unread = friendUnread.getOrDefault(username, 0);
            boolean online = friendOnline.getOrDefault(username, false);
            Long typingUntil = friendTypingUntil.get(username);
            boolean typing = typingUntil != null && typingUntil > now;
            if (typingUntil != null && typingUntil <= now) {
                friendTypingUntil.remove(username);
            }
            String preview = typing ? "typing..." : friendLastPreview.getOrDefault(username, "");
            String display = username
                    + (activePreferences.isOnlineStatus() ? (online ? " ●" : "") : "")
                    + (unread > 0 ? " (" + unread + ")" : "")
                    + (!preview.isBlank() ? " - " + preview : "");
            friendDisplayToUsername.put(display, username);
            userListView.getItems().add(display);
            if (selectedUser != null && selectedUser.equals(username)) {
                displayToSelect = display;
            }
        }

        if (displayToSelect != null) {
            userListView.getSelectionModel().select(displayToSelect);
        }
    }

    private void renderRoomList() {
        String selectedRoomId = roomTarget;
        roomDisplayToId.clear();
        roomListView.getItems().clear();

        List<String> roomIds = new ArrayList<>(roomIdToName.keySet());
        roomIds.sort(Comparator
                .comparing((String id) -> roomLastActivity.getOrDefault(id, LocalDateTime.MIN)).reversed()
                .thenComparing(id -> roomIdToName.getOrDefault(id, "").toLowerCase()));

        String displayToSelect = null;
        for (String roomId : roomIds) {
            String name = roomIdToName.getOrDefault(roomId, roomId);
            int unread = roomUnread.getOrDefault(roomId, 0);
            String display = name + (unread > 0 ? " (" + unread + ")" : "");
            roomDisplayToId.put(display, roomId);
            roomListView.getItems().add(display);
            if (selectedRoomId != null && selectedRoomId.equals(roomId)) {
                displayToSelect = display;
            }
        }

        if (displayToSelect != null) {
            roomListView.getSelectionModel().select(displayToSelect);
        }
    }

    private void processFriendRequestAction(String requester,
                                            boolean accept,
                                            ListView<String> list,
                                            Label hint) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                if (accept) {
                    apiService.acceptFriendRequest(username, requester);
                } else {
                    apiService.rejectFriendRequest(username, requester);
                }
                Platform.runLater(() -> {
                    list.getItems().remove(requester);
                    if (list.getItems().isEmpty()) {
                        hint.setText("No pending friend requests.");
                    }

                    if (accept) {
                        ensureFriendTracked(requester);
                        renderFriendList();
                    }

                    loadFriends();
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to update request: " + e.getMessage()));
            }
        });
    }

    private void ensureFriendTracked(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        if (!friendUsernames.contains(username)) {
            friendUsernames.add(username);
        }
        friendUnread.putIfAbsent(username, 0);
        friendLastActivity.putIfAbsent(username, LocalDateTime.MIN);
    }

    @FXML
    private void handlePreferences() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                UserPreferences prefs = apiService.getPreferences(username);
                Platform.runLater(() -> showPreferencesDialog(prefs));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to load preferences: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleBlockedUsers() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> blocked = apiService.getBlockedUsers(username);
                Platform.runLater(() -> showBlockedUsersDialog(blocked));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Failed to load blocked users: " + e.getMessage()));
            }
        });
    }

    private void showPreferencesDialog(UserPreferences prefs) {
        UserPreferences effective = prefs != null ? prefs : new UserPreferences();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Preferences");
        dialog.setHeaderText("Chat preferences");

        CheckBox darkMode = new CheckBox("Dark mode");
        darkMode.setSelected(effective.isDarkMode());

        CheckBox showReceipts = new CheckBox("Show read receipts");
        showReceipts.setSelected(effective.isShowReadReceipts());

        CheckBox notifications = new CheckBox("Enable notifications");
        notifications.setSelected(effective.isNotis());

        CheckBox onlineStatus = new CheckBox("Show online status");
        onlineStatus.setSelected(effective.isOnlineStatus());

        ComboBox<String> fontSize = new ComboBox<>();
        fontSize.getItems().addAll("Small", "Medium", "Large");
        fontSize.setValue(effective.getFontSize() <= 1 ? "Small" : (effective.getFontSize() >= 3 ? "Large" : "Medium"));

        ComboBox<String> bubbleColour = new ComboBox<>();
        bubbleColour.getItems().addAll("blue", "teal", "orange", "pink");
        bubbleColour.setValue(effective.getBubbleColour() != null ? effective.getBubbleColour() : "blue");

        VBox content = new VBox(10,
                darkMode,
                showReceipts,
                notifications,
                onlineStatus,
                new Label("Font size"),
                fontSize,
                new Label("Bubble colour"),
                bubbleColour);
        dialog.getDialogPane().setContent(content);

        ButtonType saveType = new ButtonType("Save");
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();

            UserPreferences updated = new UserPreferences();
            updated.setDarkMode(darkMode.isSelected());
            updated.setShowReadReceipts(showReceipts.isSelected());
            updated.setNotis(notifications.isSelected());
            updated.setOnlineStatus(onlineStatus.isSelected());
            updated.setLastSeen(effective.isLastSeen());
            updated.setFontSize("Small".equals(fontSize.getValue()) ? 1 : ("Large".equals(fontSize.getValue()) ? 3 : 2));
            updated.setFontStyle(effective.getFontStyle());
            updated.setBubbleColour(bubbleColour.getValue());
            updated.setStatus(effective.getStatus());
            updated.setBio(effective.getBio());

            String username = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    apiService.updatePreferences(username, updated);
                    Platform.runLater(() -> {
                        applyPreferences(updated);
                        dialog.close();
                        showInfo("Preferences updated");
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> showError("Failed to save preferences: " + ex.getMessage()));
                }
            });
        });

        dialog.showAndWait();
    }

    private void loadAndApplyPreferences() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                UserPreferences prefs = apiService.getPreferences(username);
                Platform.runLater(() -> applyPreferences(prefs));
            } catch (IOException ignored) {
                Platform.runLater(() -> applyPreferences(new UserPreferences()));
            }
        });
    }

    private void applyPreferences(UserPreferences prefs) {
        activePreferences = prefs != null ? prefs : new UserPreferences();

        chatRoot.getStyleClass().removeAll(
                "pref-dark",
                "pref-bubble-blue",
                "pref-bubble-teal",
                "pref-bubble-orange",
                "pref-bubble-pink",
                "pref-font-sm",
                "pref-font-md",
                "pref-font-lg");

        if (activePreferences.isDarkMode()) {
            chatRoot.getStyleClass().add("pref-dark");
        }

        String bubble = activePreferences.getBubbleColour() != null
                ? activePreferences.getBubbleColour().toLowerCase()
                : "blue";
        switch (bubble) {
            case "teal" -> chatRoot.getStyleClass().add("pref-bubble-teal");
            case "orange" -> chatRoot.getStyleClass().add("pref-bubble-orange");
            case "pink" -> chatRoot.getStyleClass().add("pref-bubble-pink");
            default -> chatRoot.getStyleClass().add("pref-bubble-blue");
        }

        int size = activePreferences.getFontSize();
        if (size <= 1) {
            chatRoot.getStyleClass().add("pref-font-sm");
        } else if (size >= 3) {
            chatRoot.getStyleClass().add("pref-font-lg");
        } else {
            chatRoot.getStyleClass().add("pref-font-md");
        }

        renderFriendList();
    }

    private void showBlockedUsersDialog(List<Map<String, Object>> blocked) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Blocked Users");
        dialog.setHeaderText("Manage blocked users");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username to block");

        ListView<String> blockedList = new ListView<>();
        blockedList.setPrefHeight(180);
        if (blocked != null) {
            for (Map<String, Object> item : blocked) {
                Object username = item.get("username");
                if (username != null) {
                    blockedList.getItems().add(username.toString());
                }
            }
        }

        Label hint = new Label("Select an entry to unblock.");
        VBox content = new VBox(10,
                new Label("Block a user"),
                usernameField,
                hint,
                blockedList);
        dialog.getDialogPane().setContent(content);

        ButtonType blockType = new ButtonType("Block");
        ButtonType unblockType = new ButtonType("Unblock");
        dialog.getDialogPane().getButtonTypes().addAll(blockType, unblockType, ButtonType.CLOSE);

        Button blockBtn = (Button) dialog.getDialogPane().lookupButton(blockType);
        Button unblockBtn = (Button) dialog.getDialogPane().lookupButton(unblockType);
        blockBtn.disableProperty().bind(usernameField.textProperty().isEmpty());
        unblockBtn.disableProperty().bind(blockedList.getSelectionModel().selectedItemProperty().isNull());

        blockBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String target = usernameField.getText() != null ? usernameField.getText().trim() : "";
            if (target.isEmpty()) {
                return;
            }

            String username = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    apiService.blockUser(username, target);
                    Platform.runLater(() -> {
                        if (!blockedList.getItems().contains(target)) {
                            blockedList.getItems().add(target);
                        }
                        usernameField.clear();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> showError("Failed to block user: " + ex.getMessage()));
                }
            });
        });

        unblockBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String target = blockedList.getSelectionModel().getSelectedItem();
            if (target == null) {
                return;
            }

            String username = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    apiService.unblockUser(username, target);
                    Platform.runLater(() -> blockedList.getItems().remove(target));
                } catch (IOException ex) {
                    Platform.runLater(() -> showError("Failed to unblock user: " + ex.getMessage()));
                }
            });
        });

        dialog.showAndWait();
    }
}
