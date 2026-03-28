package com.chitchat.client.controller;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.chitchat.client.ChatClientApp;
import com.chitchat.client.model.UserSession;
import com.chitchat.client.service.ApiService;
import com.chitchat.client.service.WebSocketService;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;
import com.chitchat.shared.UserPreferences;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
    @FXML private Button gifButton;
    @FXML private Button micButton;
    @FXML private Button locationButton;

    private final WebSocketService wsService = new WebSocketService();
    private final ApiService apiService = new ApiService(ChatClientApp.SERVER_URL);
    private final ObjectMapper mapper = new ObjectMapper();

    // Avatar colours — same palette as web
    private static final String[] AVATAR_COLORS = {
        "#4c6fff","#ff6b9d","#f59e0b","#10b981","#8b5cf6","#ef4444","#06b6d4","#84cc16"
    };
    private static final String TENOR_KEY = "LIVDSRZULELA";

    private static final int MESSAGE_DEDUPE_WINDOW = 800;
    private static final long NOTIFICATION_COOLDOWN_MS = 1500;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dayFmt  = DateTimeFormatter.ofPattern("EEE HH:mm");
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final PauseTransition typingDebounce = new PauseTransition(Duration.millis(500));
    private final PauseTransition typingIndicatorTimeout = new PauseTransition(Duration.seconds(2.8));
    private final Timeline rosterRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(8), e -> { loadFriends(); loadRooms(); }));

    // State maps
    private final Map<String, Label>          receiptStatusByMessageId = new HashMap<>();
    private final Map<String, List<String>>   reactionsByMessageId     = new HashMap<>();
    private final Map<String, String>         friendFullNames          = new HashMap<>();
    private final Map<String, Boolean>        friendOnline             = new HashMap<>();
    private final Map<String, Integer>        friendUnread             = new HashMap<>();
    private final Map<String, LocalDateTime>  friendLastActivity       = new HashMap<>();
    private final Map<String, String>         friendLastPreview        = new HashMap<>();
    private final Map<String, Long>           friendTypingUntil        = new HashMap<>();
    private final List<String>                friendUsernames          = new ArrayList<>();
    private final Map<String, String>         roomIdToName             = new HashMap<>();
    private final Map<String, Integer>        roomUnread               = new HashMap<>();
    private final Map<String, LocalDateTime>  roomLastActivity         = new HashMap<>();
    private final LinkedHashMap<String, Boolean> recentMessageKeys     = new LinkedHashMap<>();

    // "Sending → Sent" pending queue
    private final Deque<Label> pendingStatusLabels = new ArrayDeque<>();

    private String privateTarget = null;
    private String roomTarget    = null;
    private final Map<String, String> roomDisplayToId = new HashMap<>();
    private boolean loadingHistory = false;
    private String activeCallTarget = null;
    private UserPreferences activePreferences = new UserPreferences();
    private TrayIcon trayIcon;
    private boolean windowFocused = true;
    private long lastNotificationEpochMs = 0;

    // Voice recording state
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    private TargetDataLine recordingLine;

    // ─── Initialise ──────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserSession session = UserSession.getInstance();
        userInfoLabel.setText(session.getDisplayName() + " (@" + session.getUsername() + ")");
        initDesktopNotifications();
        Platform.runLater(this::bindWindowFocusTracking);

        // Custom cell factories for sidebar lists
        userListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                setGraphic(buildFriendCell(username));
                setStyle("-fx-padding: 4 6;");
            }
        });

        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String roomId, boolean empty) {
                super.updateItem(roomId, empty);
                if (empty || roomId == null) { setGraphic(null); return; }
                setGraphic(buildRoomCell(roomId));
                setStyle("-fx-padding: 4 6;");
            }
        });

        // Click on friend → open private chat (click again to deselect)
        userListView.setOnMouseClicked(e -> {
            String selected = userListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (selected.equals(session.getUsername())) return;

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
        messageInput.textProperty().addListener((obs, old, val) -> {
            if (privateTarget == null) return;
            if (val == null || val.isBlank()) { typingDebounce.stop(); sendTypingStopped(); return; }
            typingDebounce.playFromStart();
        });

        loadFriends();
        loadRooms();
        loadPublicHistory();
        loadAndApplyPreferences();

        rosterRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        rosterRefreshTimeline.play();
    }

    // ─── Avatar helpers ───────────────────────────────────────────────────────

    private String avatarColor(String username) {
        int h = 0;
        for (char c : (username == null ? "?" : username).toCharArray())
            h = (h * 31 + c) % AVATAR_COLORS.length;
        return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
    }

    private StackPane buildAvatarNode(String username, double size) {
        Circle bg = new Circle(size / 2);
        bg.setFill(Color.web(avatarColor(username == null ? "?" : username)));

        String init = (username == null || username.isEmpty()) ? "?" :
                String.valueOf(username.charAt(0)).toUpperCase();
        Label lbl = new Label(init);
        lbl.setStyle(String.format(
            "-fx-font-size: %.0fpx; -fx-font-weight: bold; -fx-text-fill: white;",
            size * 0.40));

        StackPane sp = new StackPane(bg, lbl);
        sp.setPrefSize(size, size);
        sp.setMaxSize(size, size);
        sp.setMinSize(size, size);
        return sp;
    }

    // ─── Custom sidebar cells ─────────────────────────────────────────────────

    private HBox buildFriendCell(String username) {
        StackPane avatar = buildAvatarNode(username, 36);

        String fullName = friendFullNames.getOrDefault(username, username);
        boolean isTyping = friendTypingUntil.getOrDefault(username, 0L) > System.currentTimeMillis();
        String preview = isTyping ? "typing…" : friendLastPreview.getOrDefault(username, "");
        int unread = friendUnread.getOrDefault(username, 0);
        boolean online = friendOnline.getOrDefault(username, false);

        Label nameLbl = new Label(fullName + (online && activePreferences.isOnlineStatus() ? " ●" : ""));
        nameLbl.getStyleClass().add("friend-name");
        nameLbl.setMaxWidth(Double.MAX_VALUE);

        Label previewLbl = new Label(preview);
        previewLbl.getStyleClass().add(isTyping ? "friend-typing" : "friend-preview");
        previewLbl.setMaxWidth(Double.MAX_VALUE);

        VBox nameBox = new VBox(1, nameLbl, previewLbl);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox row = new HBox(8, avatar, nameBox);
        row.setAlignment(Pos.CENTER_LEFT);

        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread));
            badge.getStyleClass().add("badge-unread");
            row.getChildren().add(badge);
        }
        return row;
    }

    private HBox buildRoomCell(String roomId) {
        String name = roomIdToName.getOrDefault(roomId, roomId);
        int unread = roomUnread.getOrDefault(roomId, 0);

        // Room uses a hash (#) avatar in yellow
        Circle bg = new Circle(18);
        bg.setFill(Color.web("#fff685"));
        Label hash = new Label("#");
        hash.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1c1c1c;");
        StackPane avatar = new StackPane(bg, hash);
        avatar.setPrefSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setMinSize(36, 36);

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #e8f1f2;");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        HBox row = new HBox(8, avatar, nameLbl);
        row.setAlignment(Pos.CENTER_LEFT);

        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread));
            badge.getStyleClass().add("badge-unread");
            row.getChildren().add(badge);
        }
        return row;
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

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
        if (file == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                String payload = createImagePayload(file);
                Platform.runLater(() -> sendContent(payload));
            } catch (IOException ex) {
                Platform.runLater(() -> showError("Failed to attach image: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void handleSendGif() {
        Stage gifStage = new Stage();
        gifStage.setTitle("Send a GIF");
        gifStage.initModality(Modality.APPLICATION_MODAL);
        if (chatRoot.getScene() != null)
            gifStage.initOwner(chatRoot.getScene().getWindow());

        TextField search = new TextField();
        search.setPromptText("Search GIFs…");

        FlowPane grid = new FlowPane(4, 4);
        grid.setPrefWrapLength(460);
        grid.getStyleClass().add("gif-grid");

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background-color: #f5f5f5;");

        Label status = new Label("Loading trending GIFs…");
        status.setStyle("-fx-padding: 8; -fx-text-fill: #999;");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-primary");
        searchBtn.setOnAction(e -> fetchGifs(search.getText().trim(), grid, status, gifStage));
        search.setOnAction(e -> fetchGifs(search.getText().trim(), grid, status, gifStage));

        HBox top = new HBox(8, search, searchBtn);
        top.setPadding(new Insets(10));
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox root = new VBox(top, status, scroll);
        Scene scene = new Scene(root, 490, 400);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        gifStage.setScene(scene);
        gifStage.show();

        // Load trending immediately
        fetchGifs("", grid, status, gifStage);
    }

    private void fetchGifs(String query, FlowPane grid, Label status, Stage gifStage) {
        grid.getChildren().clear();
        status.setText("Loading…");
        String apiUrl;
        if (query.isBlank()) {
            apiUrl = "https://tenor.googleapis.com/v2/featured?key=" + TENOR_KEY
                    + "&limit=24&media_filter=tinygif,gif";
        } else {
            String encodedQ = URLEncoder.encode(query, StandardCharsets.UTF_8);
            apiUrl = "https://tenor.googleapis.com/v2/search?q=" + encodedQ
                    + "&key=" + TENOR_KEY + "&limit=24&media_filter=tinygif,gif";
        }

        CompletableFuture.runAsync(() -> {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder().url(apiUrl).build();
                try (okhttp3.Response resp = apiService.getHttp().newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "{}";
                    JsonNode root = mapper.readTree(body);
                    JsonNode results = root.path("results");
                    List<String[]> gifEntries = new ArrayList<>(); // [thumbUrl, fullUrl]
                    for (JsonNode r : results) {
                        JsonNode formats = r.path("media_formats");
                        // Prefer tinygif for thumbnail display, fall back to gif
                        String thumbUrl = formats.path("tinygif").path("url").asText();
                        String fullUrl  = formats.path("gif").path("url").asText();
                        if (thumbUrl.isBlank()) thumbUrl = fullUrl;
                        if (fullUrl.isBlank())  fullUrl  = thumbUrl;
                        if (!fullUrl.isBlank()) gifEntries.add(new String[]{thumbUrl, fullUrl});
                    }
                    final List<String[]> entries = gifEntries;
                    Platform.runLater(() -> {
                        status.setText("");
                        grid.getChildren().clear();
                        if (entries.isEmpty()) {
                            status.setText("No GIFs found.");
                            return;
                        }
                        for (String[] entry : entries) {
                            String thumbUrl = entry[0], fullUrl = entry[1];
                            Image img = new Image(thumbUrl, 140, 100, true, true, true);
                            ImageView iv = new ImageView(img);
                            iv.setFitWidth(140);
                            iv.setFitHeight(100);
                            iv.getStyleClass().add("gif-thumb");
                            iv.setOnMouseClicked(e -> {
                                gifStage.close();
                                sendContent(fullUrl);
                            });
                            grid.getChildren().add(iv);
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("Failed to load GIFs: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void handleVoiceRecord() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                showError("Microphone not available on this device.");
                return;
            }
            recordingLine = (TargetDataLine) AudioSystem.getLine(info);
            recordingLine.open(format);
            recordingLine.start();
            isRecording = true;
            micButton.setText("⏹");
            micButton.getStyleClass().add("btn-recording");
            micButton.getStyleClass().remove("btn-attach");

            TargetDataLine line = recordingLine;
            AudioFormat fmt = format;
            recordingThread = new Thread(() -> {
                ByteArrayOutputStream raw = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                while (isRecording) {
                    int n = line.read(buf, 0, buf.length);
                    if (n > 0) raw.write(buf, 0, n);
                }
                line.stop();
                line.close();
                byte[] pcm = raw.toByteArray();
                try {
                    AudioInputStream ais = new AudioInputStream(
                            new ByteArrayInputStream(pcm), fmt,
                            pcm.length / fmt.getFrameSize());
                    ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
                    AudioSystem.write(ais, javax.sound.sampled.AudioFileFormat.Type.WAVE, wavOut);
                    String b64 = java.util.Base64.getEncoder().encodeToString(wavOut.toByteArray());
                    String payload = "data:audio/wav;base64," + b64;
                    Platform.runLater(() -> {
                        resetMicButton();
                        sendContent(payload);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> { resetMicButton(); showError("Failed to encode recording."); });
                }
            });
            recordingThread.setDaemon(true);
            recordingThread.start();
        } catch (Exception ex) {
            showError("Failed to start recording: " + ex.getMessage());
        }
    }

    private void stopRecording() {
        isRecording = false;
    }

    @FXML
    private void handleSendLocation() {
        locationButton.setDisable(true);
        locationButton.setText("⏳");
        CompletableFuture.runAsync(() -> {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder()
                        .url("http://ip-api.com/json?fields=city,regionName,country,lat,lon,status")
                        .build();
                try (okhttp3.Response resp = apiService.getHttp().newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "{}";
                    JsonNode json = mapper.readTree(body);
                    String status = json.path("status").asText();
                    Platform.runLater(() -> {
                        locationButton.setDisable(false);
                        locationButton.setText("📍");
                        if ("success".equals(status)) {
                            String city    = json.path("city").asText("Unknown");
                            String region  = json.path("regionName").asText("");
                            String country = json.path("country").asText("");
                            double lat     = json.path("lat").asDouble();
                            double lon     = json.path("lon").asDouble();
                            String msg = "📍 " + city + (region.isBlank() ? "" : ", " + region)
                                    + ", " + country + " (" + lat + ", " + lon + ")"
                                    + " — https://maps.google.com/?q=" + lat + "," + lon;
                            sendContent(msg);
                        } else {
                            showError("Could not detect location.");
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    locationButton.setDisable(false);
                    locationButton.setText("📍");
                    showError("Location error: " + ex.getMessage());
                });
            }
        });
    }

    private void resetMicButton() {
        micButton.setText("🎤");
        micButton.getStyleClass().remove("btn-recording");
        if (!micButton.getStyleClass().contains("btn-attach"))
            micButton.getStyleClass().add("btn-attach");
    }

    private void sendContent(String content) {
        if (content == null || content.isBlank()) return;

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

        // Show immediately with "Sending…" status
        Label statusLbl = new Label("Sending…");
        statusLbl.getStyleClass().add("msg-status-sending");
        displayLocalMessage(msg, statusLbl);

        // For private / public messages, queue the status label for upgrade to "Sent"
        if (msg.getType() == MessageType.PRIVATE_MESSAGE || msg.getType() == MessageType.PUBLIC_MESSAGE) {
            synchronized (pendingStatusLabels) {
                pendingStatusLabels.addLast(statusLbl);
            }
        }

        wsService.sendMessage(msg);
    }

    /** Immediately displays the user's own outgoing message (before server echo). */
    private void displayLocalMessage(Message msg, Label statusLbl) {
        HBox row = new HBox();
        row.setPadding(new Insets(2, 12, 2, 12));

        VBox bubble = new VBox(2);

        // For private chat show "You → Name (private)"
        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            String targetDisplay = friendFullNames.getOrDefault(msg.getReceiver(), msg.getReceiver());
            Label senderLabel = new Label("You → " + targetDisplay + " (private)");
            senderLabel.getStyleClass().add("msg-sender");
            bubble.getChildren().add(senderLabel);
        }

        Label content = new Label();
        content.getStyleClass().addAll("msg-bubble", "msg-mine");
        if (msg.getType() == MessageType.PRIVATE_MESSAGE)
            content.getStyleClass().add("msg-private");

        Node msgNode = buildContentNode(msg.getContent(), content);
        content.setGraphic(msgNode);
        bubble.getChildren().add(content);

        Label timeLbl = new Label("Just now");
        timeLbl.getStyleClass().add("msg-time");
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_RIGHT);
        meta.getChildren().addAll(timeLbl, statusLbl);
        bubble.getChildren().add(meta);

        row.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);

        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(e -> Platform.runLater(() -> messagesScroll.setVvalue(1.0)));
        delay.play();
    }

    // ─── Incoming messages ────────────────────────────────────────────────────

    private void handleIncomingMessage(Message msg) {
        if (msg == null || msg.getType() == null) return;

        if (msg.getType() == MessageType.TYPING) { handleTypingSignal(msg); return; }
        if (msg.getType() == MessageType.READ_RECEIPT) { handleReadReceipt(msg); return; }
        if (msg.getType() == MessageType.CALL_OFFER || msg.getType() == MessageType.CALL_ANSWER
                || msg.getType() == MessageType.CALL_END || msg.getType() == MessageType.CALL_REJECT) {
            handleCallSignal(msg); return;
        }

        if (!acceptMessageIfNew(msg)) return;

        maybeClearTypingIndicatorOnIncoming(msg);
        trackMessageActivity(msg);

        // Check if this is the server echo of our own pending "Sending…" message
        String me = UserSession.getInstance().getUsername();
        boolean isMine = me.equals(msg.getSender());
        boolean isPendingType = msg.getType() == MessageType.PRIVATE_MESSAGE
                || msg.getType() == MessageType.PUBLIC_MESSAGE;

        if (isMine && isPendingType) {
            synchronized (pendingStatusLabels) {
                if (!pendingStatusLabels.isEmpty()) {
                    Label statusLbl = pendingStatusLabels.pollFirst();
                    // Upgrade "Sending…" → "Sent ✓" and fill in real time
                    statusLbl.setText("Sent ✓");
                    statusLbl.getStyleClass().remove("msg-status-sending");
                    statusLbl.getStyleClass().add("msg-status-sent");
                    // Also update the time placeholder next to status
                    if (statusLbl.getParent() instanceof HBox meta && msg.getTimestamp() != null) {
                        for (Node n : meta.getChildren()) {
                            if (n instanceof Label tl && tl.getStyleClass().contains("msg-time")) {
                                tl.setText(formatRelativeTime(msg.getTimestamp()));
                            }
                        }
                        // If first child is empty placeholder, set it to the time
                        if (!meta.getChildren().isEmpty() && meta.getChildren().get(0) instanceof Label tl && tl.getText().isBlank()) {
                            tl.setText(formatRelativeTime(msg.getTimestamp()));
                            tl.getStyleClass().add("msg-time");
                        }
                    }
                    return; // don't render duplicate
                }
            }
        }

        boolean willDisplay = shouldDisplayMessage(msg);
        maybeNotifyIncomingMessage(msg, willDisplay);

        if (!willDisplay) return;

        displayMessage(msg, false);
    }

    // ─── Display message ──────────────────────────────────────────────────────

    private void displayMessage(Message msg, boolean fromHistory) {
        UserSession session = UserSession.getInstance();
        boolean isMine   = msg.getSender().equals(session.getUsername());
        boolean isSystem  = "System".equals(msg.getSender());
        boolean isPrivate = msg.getType() == MessageType.PRIVATE_MESSAGE;

        HBox row = new HBox();
        row.setPadding(new Insets(2, 12, 2, 12));

        if (isSystem) {
            Label sysLabel = new Label(msg.getContent());
            sysLabel.getStyleClass().add("msg-system");
            row.setAlignment(Pos.CENTER);
            row.getChildren().add(sysLabel);
        } else {
            // Avatar for others' messages (left side)
            if (!isMine) {
                StackPane avatar = buildAvatarNode(msg.getSender(), 32);
                avatar.setStyle("-fx-margin-right:6;");
                row.getChildren().add(avatar);
                HBox.setMargin(avatar, new Insets(0, 6, 0, 0));
            }

            VBox bubble = new VBox(2);
            if (!isMine) {
                // Show full name as sender
                String senderDisplay = isPrivate
                        ? (friendFullNames.getOrDefault(msg.getSender(), msg.getSender()) + " → You (private)")
                        : friendFullNames.getOrDefault(msg.getSender(), msg.getSender());
                Label senderLabel = new Label(senderDisplay);
                senderLabel.getStyleClass().add("msg-sender");
                bubble.getChildren().add(senderLabel);
            } else if (isPrivate) {
                String targetDisplay = friendFullNames.getOrDefault(msg.getReceiver(), msg.getReceiver());
                Label senderLabel = new Label("You → " + targetDisplay + " (private)");
                senderLabel.getStyleClass().add("msg-sender");
                bubble.getChildren().add(senderLabel);
            }

            Label content = new Label();
            content.getStyleClass().addAll("msg-bubble",
                    isMine ? "msg-mine" : "msg-theirs");
            if (isPrivate) content.getStyleClass().add("msg-private");

            Node msgNode = buildContentNode(msg.getContent(), content);
            content.setGraphic(msgNode);

            attachReactionMenu(content, bubble, msg);
            // Add "Delete" to the context menu for the sender's own messages
            if (isMine && msg.getId() != null && !msg.getId().isBlank()) {
                HBox finalRow = row;
                ContextMenu cm = content.getContextMenu();
                if (cm == null) { cm = new ContextMenu(); content.setContextMenu(cm); }
                MenuItem deleteItem = new MenuItem("🗑 Delete message");
                final String msgId = msg.getId();
                deleteItem.setOnAction(ev -> {
                    String username = UserSession.getInstance().getUsername();
                    CompletableFuture.runAsync(() -> {
                        try {
                            apiService.deleteMessage(msgId, username);
                            Platform.runLater(() -> messagesBox.getChildren().remove(finalRow));
                        } catch (IOException ex) {
                            Platform.runLater(() -> showError("Delete failed: " + ex.getMessage()));
                        }
                    });
                });
                cm.getItems().add(new SeparatorMenuItem());
                cm.getItems().add(deleteItem);
            }
            bubble.getChildren().add(content);

            HBox meta = new HBox(6);
            meta.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            Label time = new Label(formatRelativeTime(msg.getTimestamp()));
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
        if (!fromHistory) {
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> Platform.runLater(() -> messagesScroll.setVvalue(1.0)));
            delay.play();
        }
    }

    // ─── Content node builder ─────────────────────────────────────────────────

    private Node buildContentNode(String rawContent, Label wrapper) {
        if (isImagePayload(rawContent)) return buildImageNode(rawContent);
        if (isGifUrl(rawContent))        return buildGifNode(rawContent);
        if (isAudioPayload(rawContent))  return buildAudioNode(rawContent);

        Text text = new Text(rawContent != null ? rawContent : "");
        text.setWrappingWidth(340);
        return text;
    }

    private boolean isImagePayload(String c) {
        return c != null && c.startsWith("data:image/") && c.contains(";base64,");
    }

    private boolean isGifUrl(String c) {
        return c != null && (c.startsWith("https://media.tenor.com/")
                || c.startsWith("https://c.tenor.com/")
                || (c.startsWith("https://") && c.contains("tenor") && c.endsWith(".gif")));
    }

    private boolean isAudioPayload(String c) {
        return c != null && c.startsWith("data:audio/");
    }

    private Node buildImageNode(String payload) {
        try {
            int comma = payload.indexOf(',');
            if (comma < 0) return new Text("[Invalid image]");
            byte[] data = java.util.Base64.getDecoder().decode(payload.substring(comma + 1));
            Image image = new Image(new ByteArrayInputStream(data));
            ImageView iv = new ImageView(image);
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("msg-image");
            return iv;
        } catch (Exception ex) {
            return new Text("[Unable to render image]");
        }
    }

    private Node buildGifNode(String url) {
        try {
            Image img = new Image(url, 220, 160, true, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("msg-image");
            return iv;
        } catch (Exception ex) {
            return new Text("[GIF: " + url + "]");
        }
    }

    private Node buildAudioNode(String payload) {
        Button playBtn = new Button("▶  Voice message");
        playBtn.getStyleClass().add("btn-audio");
        playBtn.setOnAction(e -> {
            playBtn.setDisable(true);
            CompletableFuture.runAsync(() -> {
                try {
                    int comma = payload.indexOf(',');
                    byte[] data = java.util.Base64.getDecoder().decode(payload.substring(comma + 1));
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    clip.start();
                    clip.addLineListener(ev -> {
                        if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                            Platform.runLater(() -> playBtn.setDisable(false));
                            clip.close();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        playBtn.setDisable(false);
                        showError("Cannot play audio: " + ex.getMessage());
                    });
                }
            });
        });
        return playBtn;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String formatRelativeTime(LocalDateTime ts) {
        if (ts == null) return "";
        long secs = java.time.Duration.between(ts, LocalDateTime.now()).getSeconds();
        // Treat small negative values (server in different TZ, up to ±2min) as "Just now"
        if (Math.abs(secs) < 120) return "Just now";
        if (secs < 0)      return ts.format(timeFmt); // server clock ahead — just show time
        if (secs < 3600)   return (secs / 60) + " min ago";
        if (secs < 86400)  return (secs / 3600) + " hr ago";
        if (secs < 172800) return "Yesterday " + ts.format(timeFmt);
        if (secs < 604800) return ts.format(dayFmt);
        return ts.format(dateFmt);
    }

    private String createImagePayload(File file) throws IOException {
        BufferedImage original = ImageIO.read(file);
        if (original == null) throw new IOException("Unreadable image file.");

        int w = original.getWidth(), h = original.getHeight();
        int maxDim = 1200;
        if (w > maxDim || h > maxDim) {
            double scale = Math.min((double) maxDim / w, (double) maxDim / h);
            int nw = (int) (w * scale), nh = (int) (h * scale);
            BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = resized.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, nw, nh, null);
            g.dispose();
            original = resized;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "jpeg", baos);
        String encoded = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/jpeg;base64," + encoded;
    }

    // ─── Typing ───────────────────────────────────────────────────────────────

    private void sendTypingIndicator() { sendTypingSignal("typing"); }
    private void sendTypingStopped()   { sendTypingSignal("stopped"); }

    private void sendTypingSignal(String state) {
        if (privateTarget == null) return;
        String sender = UserSession.getInstance().getUsername();
        wsService.sendMessage(new Message(MessageType.TYPING, sender, privateTarget, state));
    }

    private void handleTypingSignal(Message msg) {
        if (privateTarget == null || msg.getSender() == null || !msg.getSender().equals(privateTarget)) return;
        String state = msg.getContent() != null ? msg.getContent().trim().toLowerCase() : "typing";
        if ("stopped".equals(state)) {
            friendTypingUntil.remove(msg.getSender());
            renderFriendList();
            clearTypingIndicator();
            return;
        }
        friendTypingUntil.put(msg.getSender(), System.currentTimeMillis() + 3000);
        renderFriendList();
        typingIndicatorLabel.setManaged(true);
        typingIndicatorLabel.setVisible(true);
        String name = friendFullNames.getOrDefault(privateTarget, privateTarget);
        typingIndicatorLabel.setText(name + " is typing…");
        typingIndicatorTimeout.playFromStart();
    }

    private void maybeClearTypingIndicatorOnIncoming(Message msg) {
        if (msg.getType() != MessageType.PRIVATE_MESSAGE || privateTarget == null) return;
        String me = UserSession.getInstance().getUsername();
        if (privateTarget.equals(msg.getSender()) && me.equals(msg.getReceiver())) {
            friendTypingUntil.remove(msg.getSender());
            renderFriendList();
            clearTypingIndicator();
        }
    }

    private void clearTypingIndicator() {
        typingIndicatorLabel.setText("");
        typingIndicatorLabel.setManaged(false);
        typingIndicatorLabel.setVisible(false);
    }

    // ─── Read receipts ────────────────────────────────────────────────────────

    private void handleReadReceipt(Message msg) {
        if (!activePreferences.isShowReadReceipts()) return;
        if (msg.getContent() == null || msg.getContent().isBlank()) return;
        Label status = receiptStatusByMessageId.get(msg.getContent());
        if (status != null) status.setText("Seen");
    }

    private void sendReadReceipt(String messageId) {
        if (!activePreferences.isShowReadReceipts()) return;
        if (loadingHistory || messageId == null || messageId.isBlank()) return;
        String sender = UserSession.getInstance().getUsername();
        Message receipt = new Message(MessageType.READ_RECEIPT, sender, null, messageId);
        receipt.setId(messageId);
        wsService.sendMessage(receipt);
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    private void initDesktopNotifications() {
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            trayIcon = new TrayIcon(img, "ChitChat");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (AWTException | SecurityException ignored) { trayIcon = null; }
    }

    private void bindWindowFocusTracking() {
        if (chatRoot == null || chatRoot.getScene() == null) return;
        Stage stage = (Stage) chatRoot.getScene().getWindow();
        if (stage == null) return;
        windowFocused = stage.isFocused();
        stage.focusedProperty().addListener((obs, old, focused) -> windowFocused = focused);
    }

    private void maybeNotifyIncomingMessage(Message msg, boolean willDisplay) {
        if (!activePreferences.isNotis() || msg == null || msg.getType() == null) return;
        if (msg.getType() != MessageType.PUBLIC_MESSAGE
                && msg.getType() != MessageType.PRIVATE_MESSAGE
                && msg.getType() != MessageType.ROOM_MESSAGE) return;
        String me = UserSession.getInstance().getUsername();
        if (me.equals(msg.getSender())) return;
        if (windowFocused && willDisplay) return;
        long now = System.currentTimeMillis();
        if (now - lastNotificationEpochMs < NOTIFICATION_COOLDOWN_MS) return;
        lastNotificationEpochMs = now;

        String title;
        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            String name = friendFullNames.getOrDefault(msg.getSender(), "@" + msg.getSender());
            title = "Message from " + name;
        } else if (msg.getType() == MessageType.ROOM_MESSAGE) {
            title = "Room #" + getRoomDisplayName(msg.getRoomId());
        } else {
            title = "Public: @" + msg.getSender();
        }

        String raw  = msg.getContent() != null ? msg.getContent() : "";
        String base = isImagePayload(raw) ? "📷 Photo" : isGifUrl(raw) ? "GIF" : isAudioPayload(raw) ? "🎤 Voice" : raw;
        String preview = base.length() > 120 ? base.substring(0, 120) + "…" : base;
        if (trayIcon != null) trayIcon.displayMessage(title, preview, TrayIcon.MessageType.NONE);
    }

    // ─── Calls ────────────────────────────────────────────────────────────────

    @FXML private void handleVoiceCall() { startCall(false); }
    @FXML private void handleVideoCall() { startCall(true); }
    @FXML private void handleEndCall()   { endCall("Call ended"); }

    private void startCall(boolean withVideo) {
        if (privateTarget == null || privateTarget.isBlank()) return;
        if (activeCallTarget != null) { showInfo("Already in a call with " + friendFullNames.getOrDefault(activeCallTarget, activeCallTarget)); return; }
        String sender = UserSession.getInstance().getUsername();
        wsService.sendMessage(new Message(MessageType.CALL_OFFER, sender, privateTarget, withVideo ? "VIDEO" : "VOICE"));
        activeCallTarget = privateTarget;
        updateCallControls();
        String name = friendFullNames.getOrDefault(privateTarget, privateTarget);
        showInfo((withVideo ? "Video" : "Voice") + " call offer sent to " + name);
    }

    private void endCall(String statusMessage) {
        if (activeCallTarget == null || activeCallTarget.isBlank()) return;
        String sender = UserSession.getInstance().getUsername();
        wsService.sendMessage(new Message(MessageType.CALL_END, sender, activeCallTarget, ""));
        activeCallTarget = null;
        updateCallControls();
        showInfo(statusMessage);
    }

    private void handleCallSignal(Message msg) {
        String me = UserSession.getInstance().getUsername();
        if (msg.getReceiver() != null && !me.equals(msg.getReceiver())) return;

        if (msg.getType() == MessageType.CALL_OFFER) {
            String callerName = friendFullNames.getOrDefault(msg.getSender(), "@" + msg.getSender());
            Alert incoming = new Alert(Alert.AlertType.CONFIRMATION);
            incoming.setTitle("Incoming Call");
            incoming.setHeaderText("Incoming " + ("VIDEO".equalsIgnoreCase(msg.getContent()) ? "video" : "voice")
                    + " call from " + callerName);
            incoming.setContentText("Accept this call?");
            ButtonType accept = new ButtonType("Accept");
            ButtonType reject = new ButtonType("Reject");
            incoming.getButtonTypes().setAll(accept, reject);
            incoming.showAndWait().ifPresent(choice -> {
                if (choice == accept) {
                    wsService.sendMessage(new Message(MessageType.CALL_ANSWER, me, msg.getSender(), "accepted"));
                    activeCallTarget = msg.getSender();
                    updateCallControls();
                    showInfo("Call connected with " + callerName);
                } else {
                    wsService.sendMessage(new Message(MessageType.CALL_REJECT, me, msg.getSender(), "rejected"));
                }
            });
            return;
        }

        if (msg.getType() == MessageType.CALL_ANSWER) {
            activeCallTarget = msg.getSender();
            updateCallControls();
            String name = friendFullNames.getOrDefault(msg.getSender(), msg.getSender());
            showInfo(name + " accepted your call");
            return;
        }

        if (msg.getType() == MessageType.CALL_REJECT) {
            if (msg.getSender() != null && msg.getSender().equals(activeCallTarget)) {
                activeCallTarget = null; updateCallControls();
            }
            String name = friendFullNames.getOrDefault(msg.getSender(), msg.getSender());
            showInfo(name + " rejected the call");
            return;
        }

        if (msg.getType() == MessageType.CALL_END) {
            if (msg.getSender() != null && msg.getSender().equals(activeCallTarget)) {
                activeCallTarget = null; updateCallControls();
            }
            String name = friendFullNames.getOrDefault(msg.getSender(), msg.getSender());
            showInfo(name + " ended the call");
        }
    }

    private void updateCallControls() {
        boolean privateChat = privateTarget != null;
        boolean inCall = activeCallTarget != null;
        callVoiceButton.setManaged(privateChat && !inCall);
        callVoiceButton.setVisible(privateChat && !inCall);
        callVideoButton.setManaged(privateChat && !inCall);
        callVideoButton.setVisible(privateChat && !inCall);
        endCallButton.setManaged(inCall);
        endCallButton.setVisible(inCall);
    }

    private void updateRoomControls() {
        boolean inRoom = roomTarget != null;
        leaveRoomButton.setManaged(inRoom);
        leaveRoomButton.setVisible(inRoom);
    }

    // ─── Chat header ──────────────────────────────────────────────────────────

    private void updateChatHeader() {
        if (roomTarget != null) {
            chatHeaderLabel.setText("# " + getRoomDisplayName(roomTarget));
            return;
        }
        if (privateTarget != null) {
            String fullName = friendFullNames.getOrDefault(privateTarget, privateTarget);
            chatHeaderLabel.setText(fullName);
            return;
        }
        chatHeaderLabel.setText("Public Chat");
    }

    // ─── Friend & Room lists ──────────────────────────────────────────────────

    private void updateUserList(String csv) {
        friendOnline.clear();
        if (csv != null && !csv.isBlank()) {
            for (String u : csv.split(",")) {
                if (!u.isBlank()) friendOnline.put(u.trim(), true);
            }
        }
        renderFriendList();
    }

    private void renderFriendList() {
        String selected = privateTarget;
        userListView.getItems().clear();

        List<String> ordered = new ArrayList<>(friendUsernames);
        ordered.sort(Comparator
                .comparing((String u) -> friendOnline.getOrDefault(u, false)).reversed()
                .thenComparing((String u) -> friendLastActivity.getOrDefault(u, LocalDateTime.MIN)).reversed()
                .thenComparing(u -> u.toLowerCase()));

        userListView.getItems().addAll(ordered);
        if (selected != null) userListView.getSelectionModel().select(selected);
    }

    private void renderRoomList() {
        String selectedId = roomTarget;
        roomDisplayToId.clear();
        roomListView.getItems().clear();

        List<String> roomIds = new ArrayList<>(roomIdToName.keySet());
        roomIds.sort(Comparator
                .comparing((String id) -> roomLastActivity.getOrDefault(id, LocalDateTime.MIN)).reversed()
                .thenComparing(id -> roomIdToName.getOrDefault(id, "").toLowerCase()));

        String displayToSelect = null;
        for (String roomId : roomIds) {
            roomDisplayToId.put(roomId, roomId);
            roomListView.getItems().add(roomId);
            if (roomId.equals(selectedId)) displayToSelect = roomId;
        }
        if (displayToSelect != null) roomListView.getSelectionModel().select(displayToSelect);
    }

    private void loadFriends() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> friends = apiService.getFriends(username);
                Platform.runLater(() -> {
                    friendUsernames.clear();
                    for (Map<String, Object> friend : friends) {
                        Object u = friend.get("username");
                        if (u == null) continue;
                        String ustr = u.toString();
                        ensureFriendTracked(ustr);
                        String fn = stringVal(friend.get("fname"));
                        String ln = stringVal(friend.get("lname"));
                        String full = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                        if (!full.isBlank()) friendFullNames.put(ustr, full);
                    }
                    if (privateTarget != null && !privateTarget.isBlank())
                        ensureFriendTracked(privateTarget);
                    renderFriendList();
                });
            } catch (IOException ignored) {}
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
                        Object id = room.get("id"), name = room.get("name");
                        if (id == null || name == null) continue;
                        Object members = room.get("members");
                        boolean isMember = true;
                        if (members instanceof List<?> ml) {
                            isMember = ml.stream().anyMatch(m -> username.equals(String.valueOf(m)));
                        }
                        if (!isMember) continue;
                        String roomId = id.toString();
                        roomIdToName.put(roomId, name.toString());
                        roomUnread.putIfAbsent(roomId, 0);
                        roomLastActivity.putIfAbsent(roomId, LocalDateTime.MIN);
                        wsService.subscribeToRoom(roomId);
                    }
                    renderRoomList();
                    updateChatHeader();
                });
            } catch (IOException ignored) {}
        });
    }

    private void ensureFriendTracked(String username) {
        if (username == null || username.isBlank()) return;
        if (!friendUsernames.contains(username)) friendUsernames.add(username);
        friendUnread.putIfAbsent(username, 0);
        friendLastActivity.putIfAbsent(username, LocalDateTime.MIN);
    }

    private void joinRoomIfNeeded(String roomId) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try { apiService.joinRoom(Long.parseLong(roomId), username); } catch (Exception ignored) {}
        });
    }

    // ─── History ──────────────────────────────────────────────────────────────

    private void loadPublicHistory() {
        clearMessages();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getPublicMessages();
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {}
        });
    }

    private void loadPrivateHistory(String otherUser) {
        clearMessages();
        String me = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getPrivateMessages(me, otherUser);
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {}
        });
    }

    private void loadRoomHistory(String roomId) {
        clearMessages();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> raw = apiService.getRoomMessages(roomId);
                Platform.runLater(() -> renderHistory(raw));
            } catch (IOException ignored) {}
        });
    }

    private void renderHistory(List<Map<String, Object>> rawHistory) {
        loadingHistory = true;
        try {
            if (rawHistory == null) return;
            for (Map<String, Object> raw : rawHistory) {
                Message msg = toMessage(raw);
                cacheHistoryReactions(raw, msg != null ? msg.getId() : null);
                if (msg != null && shouldDisplayMessage(msg)) displayMessage(msg, true);
            }
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> Platform.runLater(() -> messagesScroll.setVvalue(1.0)));
            delay.play();
        } finally {
            loadingHistory = false;
        }
    }

    private boolean shouldDisplayMessage(Message msg) {
        MessageType type = msg.getType();
        if (type == MessageType.PUBLIC_MESSAGE)  return privateTarget == null && roomTarget == null;
        if (type == MessageType.PRIVATE_MESSAGE) {
            if (privateTarget == null) return false;
            String me = UserSession.getInstance().getUsername();
            return (me.equals(msg.getSender()) && privateTarget.equals(msg.getReceiver()))
                    || (privateTarget.equals(msg.getSender()) && me.equals(msg.getReceiver()));
        }
        if (type == MessageType.ROOM_MESSAGE) return roomTarget != null && roomTarget.equals(msg.getRoomId());
        return false;
    }

    private void trackMessageActivity(Message msg) {
        if (msg == null || msg.getType() == null) return;
        String me = UserSession.getInstance().getUsername();
        LocalDateTime at = msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now();

        if (msg.getType() == MessageType.PRIVATE_MESSAGE) {
            String other = me.equals(msg.getSender()) ? msg.getReceiver() : msg.getSender();
            if (other == null) return;
            ensureFriendTracked(other);
            friendLastActivity.put(other, at);
            friendLastPreview.put(other, toPreviewText(msg.getContent()));
            if (!me.equals(msg.getSender()) && (privateTarget == null || !privateTarget.equals(other)))
                friendUnread.put(other, friendUnread.getOrDefault(other, 0) + 1);
            renderFriendList();
            return;
        }
        if (msg.getType() == MessageType.ROOM_MESSAGE) {
            String roomId = msg.getRoomId();
            if (roomId == null) return;
            roomLastActivity.put(roomId, at);
            if (!me.equals(msg.getSender()) && (roomTarget == null || !roomTarget.equals(roomId)))
                roomUnread.put(roomId, roomUnread.getOrDefault(roomId, 0) + 1);
            renderRoomList();
        }
    }

    private void clearMessages() {
        messagesBox.getChildren().clear();
        receiptStatusByMessageId.clear();
        reactionsByMessageId.clear();
        synchronized (recentMessageKeys) { recentMessageKeys.clear(); }
        synchronized (pendingStatusLabels) { pendingStatusLabels.clear(); }
    }

    private boolean acceptMessageIfNew(Message msg) {
        MessageType type = msg.getType();
        if (type != MessageType.PUBLIC_MESSAGE && type != MessageType.PRIVATE_MESSAGE
                && type != MessageType.ROOM_MESSAGE) return true;
        String key = buildMessageDedupeKey(msg);
        synchronized (recentMessageKeys) {
            if (recentMessageKeys.containsKey(key)) return false;
            recentMessageKeys.put(key, Boolean.TRUE);
            if (recentMessageKeys.size() > MESSAGE_DEDUPE_WINDOW)
                recentMessageKeys.remove(recentMessageKeys.keySet().iterator().next());
        }
        return true;
    }

    private String buildMessageDedupeKey(Message msg) {
        if (msg.getId() != null && !msg.getId().isBlank()) return "id:" + msg.getId();
        return String.join("|",
                msg.getType() != null ? msg.getType().name() : "",
                msg.getSender() != null ? msg.getSender() : "",
                msg.getReceiver() != null ? msg.getReceiver() : "",
                msg.getRoomId() != null ? msg.getRoomId() : "",
                msg.getTimestamp() != null ? msg.getTimestamp().toString() : "",
                msg.getContent() != null ? msg.getContent() : "");
    }

    private String toPreviewText(String content) {
        if (content == null || content.isBlank()) return "";
        String p = isImagePayload(content) ? "📷 Photo"
                : isGifUrl(content) ? "GIF"
                : isAudioPayload(content) ? "🎤 Voice"
                : content.trim().replaceAll("\\s+", " ");
        return p.length() > 26 ? p.substring(0, 26) + "…" : p;
    }

    private Message toMessage(Map<String, Object> raw) {
        if (raw == null) return null;
        try {
            Message msg = new Message();
            msg.setId(stringVal(raw.get("id")));
            msg.setSender(stringVal(raw.get("sender")));
            msg.setReceiver(stringVal(raw.get("receiver")));
            msg.setContent(stringVal(raw.get("content")));
            msg.setRoomId(stringVal(raw.get("roomId")));
            String typeRaw = stringVal(raw.get("type"));
            if (typeRaw != null && !typeRaw.isBlank()) msg.setType(MessageType.valueOf(typeRaw));
            String ts = stringVal(raw.get("timestamp"));
            if (ts != null && !ts.isBlank()) msg.setTimestamp(LocalDateTime.parse(ts));
            return msg;
        } catch (Exception ignored) { return null; }
    }

    private String stringVal(Object v) { return v == null ? null : v.toString(); }

    private String getRoomDisplayName(String roomId) {
        return roomId == null ? "" : roomIdToName.getOrDefault(roomId, roomId);
    }

    // ─── Reactions ────────────────────────────────────────────────────────────

    private void attachReactionMenu(Label contentLabel, VBox bubble, Message msg) {
        if (msg == null || msg.getId() == null || msg.getId().isBlank()) return;
        ContextMenu menu = new ContextMenu();
        for (String emoji : new String[]{"❤️","😂","🔥","👍","😮","😢"}) {
            menu.getItems().add(createReactionItem(emoji, msg.getId(), bubble));
        }
        contentLabel.setContextMenu(menu);
    }

    private MenuItem createReactionItem(String emoji, String messageId, VBox bubble) {
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
        if (messageId == null || !reactionsByMessageId.containsKey(messageId)) return;
        upsertReactionLabel(bubble, messageId);
    }

    private void upsertReactionLabel(VBox bubble, String messageId) {
        List<String> reactions = reactionsByMessageId.get(messageId);
        if (reactions == null || reactions.isEmpty()) return;
        Label reactionLabel = null;
        for (Node n : bubble.getChildren()) {
            if (n instanceof Label l && l.getStyleClass().contains("msg-reactions")) {
                reactionLabel = l; break;
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
        if (messageId == null || raw == null) return;
        Object value = raw.get("reactions");
        if (!(value instanceof List<?> list)) return;
        List<String> emojis = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> rm) {
                Object emoji = rm.get("emoji");
                if (emoji != null && !emoji.toString().isBlank()) emojis.add(emoji.toString());
            }
        }
        if (!emojis.isEmpty()) reactionsByMessageId.put(messageId, emojis);
    }

    // ─── Navigation & dialogs ─────────────────────────────────────────────────

    @FXML private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Scene scene = new Scene(loader.load(), 440, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage s = new Stage();
            s.setTitle("My Profile");
            s.setScene(scene);
            s.setResizable(false);
            s.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleAddFriend()    { showAddFriendDialog(); }
    @FXML private void handleCreateRoom()   { showCreateRoomDialog(); }

    @FXML private void handleLeaveRoom() {
        if (roomTarget == null) return;
        String roomId = roomTarget, username = UserSession.getInstance().getUsername();
        String roomName = roomIdToName.getOrDefault(roomId, roomId);
        CompletableFuture.runAsync(() -> {
            try {
                apiService.leaveRoom(Long.parseLong(roomId), username);
                Platform.runLater(() -> {
                    roomTarget = null; updateChatHeader(); clearTypingIndicator();
                    loadPublicHistory(); loadRooms(); updateCallControls(); updateRoomControls();
                    showInfo("Left room #" + roomName);
                });
            } catch (Exception e) { Platform.runLater(() -> showError("Failed to leave room: " + e.getMessage())); }
        });
    }

    @FXML private void handleFriendRequests() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> pending = apiService.getPendingFriendRequests(username);
                Platform.runLater(() -> showFriendRequestsDialog(pending));
            } catch (IOException e) { Platform.runLater(() -> showError("Failed to load requests: " + e.getMessage())); }
        });
    }

    @FXML private void handleLogout() {
        rosterRefreshTimeline.stop();
        UserSession session = UserSession.getInstance();
        wsService.disconnect(session.getUsername());
        try { apiService.logout(session.getUsername()); } catch (IOException ignored) {}
        session.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 440, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) messageInput.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setTitle("ChitChat");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handlePreferences() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            UserPreferences prefs;
            try {
                prefs = apiService.getPreferences(username);
            } catch (IOException e) {
                // Fall back to cached preferences so the dialog always opens
                prefs = activePreferences != null ? activePreferences : new UserPreferences();
            }
            final UserPreferences finalPrefs = prefs;
            Platform.runLater(() -> showPreferencesDialog(finalPrefs));
        });
    }

    @FXML private void handleBlockedUsers() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> blocked = apiService.getBlockedUsers(username);
                Platform.runLater(() -> showBlockedUsersDialog(blocked));
            } catch (IOException e) { Platform.runLater(() -> showError("Failed to load blocked: " + e.getMessage())); }
        });
    }

    // ─── Preferences ─────────────────────────────────────────────────────────

    private void loadAndApplyPreferences() {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try { UserPreferences p = apiService.getPreferences(username); Platform.runLater(() -> applyPreferences(p)); }
            catch (IOException ignored) { Platform.runLater(() -> applyPreferences(new UserPreferences())); }
        });
    }

    private void applyPreferences(UserPreferences prefs) {
        activePreferences = prefs != null ? prefs : new UserPreferences();
        chatRoot.getStyleClass().removeAll("pref-dark","pref-bubble-blue","pref-bubble-teal",
                "pref-bubble-orange","pref-bubble-pink","pref-font-sm","pref-font-md","pref-font-lg");
        if (activePreferences.isDarkMode()) chatRoot.getStyleClass().add("pref-dark");
        String bubble = activePreferences.getBubbleColour() != null ? activePreferences.getBubbleColour().toLowerCase() : "blue";
        switch (bubble) {
            case "teal"   -> chatRoot.getStyleClass().add("pref-bubble-teal");
            case "orange" -> chatRoot.getStyleClass().add("pref-bubble-orange");
            case "pink"   -> chatRoot.getStyleClass().add("pref-bubble-pink");
            default       -> chatRoot.getStyleClass().add("pref-bubble-blue");
        }
        int sz = activePreferences.getFontSize();
        chatRoot.getStyleClass().add(sz <= 1 ? "pref-font-sm" : sz >= 3 ? "pref-font-lg" : "pref-font-md");
        renderFriendList();
    }

    private void showPreferencesDialog(UserPreferences prefs) {
        UserPreferences effective = prefs != null ? prefs : new UserPreferences();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Preferences"); dialog.setHeaderText("Chat preferences");

        CheckBox darkMode    = new CheckBox("Dark mode");      darkMode.setSelected(effective.isDarkMode());
        CheckBox receipts    = new CheckBox("Show read receipts"); receipts.setSelected(effective.isShowReadReceipts());
        CheckBox notifs      = new CheckBox("Enable notifications"); notifs.setSelected(effective.isNotis());
        CheckBox onlineSt    = new CheckBox("Show online status");   onlineSt.setSelected(effective.isOnlineStatus());
        ComboBox<String> sz  = new ComboBox<>(); sz.getItems().addAll("Small","Medium","Large");
        sz.setValue(effective.getFontSize() <= 1 ? "Small" : effective.getFontSize() >= 3 ? "Large" : "Medium");
        ComboBox<String> colour = new ComboBox<>(); colour.getItems().addAll("blue","teal","orange","pink");
        colour.setValue(effective.getBubbleColour() != null ? effective.getBubbleColour() : "blue");

        dialog.getDialogPane().setContent(new VBox(10, darkMode, receipts, notifs, onlineSt,
                new Label("Font size"), sz, new Label("Bubble colour"), colour));

        ButtonType saveType = new ButtonType("Save");
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ((Button) dialog.getDialogPane().lookupButton(saveType)).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            UserPreferences updated = new UserPreferences();
            updated.setDarkMode(darkMode.isSelected()); updated.setShowReadReceipts(receipts.isSelected());
            updated.setNotis(notifs.isSelected()); updated.setOnlineStatus(onlineSt.isSelected());
            updated.setLastSeen(effective.isLastSeen()); updated.setFontStyle(effective.getFontStyle());
            updated.setFontSize("Small".equals(sz.getValue()) ? 1 : "Large".equals(sz.getValue()) ? 3 : 2);
            updated.setBubbleColour(colour.getValue()); updated.setStatus(effective.getStatus()); updated.setBio(effective.getBio());
            String username = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> {
                try {
                    apiService.updatePreferences(username, updated);
                    Platform.runLater(() -> { applyPreferences(updated); dialog.close(); showInfo("Preferences saved"); });
                } catch (IOException ex) { Platform.runLater(() -> showError("Failed: " + ex.getMessage())); }
            });
        });
        dialog.showAndWait();
    }

    // ─── Friend request dialogs ───────────────────────────────────────────────

    private void showFriendRequestsDialog(List<Map<String, Object>> pending) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Friend Requests"); dialog.setHeaderText("Pending requests");
        ListView<String> list = new ListView<>(); list.setPrefHeight(220);
        if (pending != null) {
            for (Map<String, Object> item : pending) {
                Object u = item.get("username");
                if (u != null) list.getItems().add(u.toString());
            }
        }
        Label hint = new Label(list.getItems().isEmpty() ? "No pending friend requests." : "Select and Accept or Reject.");
        dialog.getDialogPane().setContent(new VBox(10, hint, list));
        ButtonType acceptType = new ButtonType("Accept"), rejectType = new ButtonType("Reject");
        dialog.getDialogPane().getButtonTypes().addAll(acceptType, rejectType, ButtonType.CLOSE);
        Button acceptBtn = (Button) dialog.getDialogPane().lookupButton(acceptType);
        Button rejectBtn = (Button) dialog.getDialogPane().lookupButton(rejectType);
        acceptBtn.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        rejectBtn.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        acceptBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> { e.consume(); processFriendRequestAction(list.getSelectionModel().getSelectedItem(), true, list, hint); });
        rejectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> { e.consume(); processFriendRequestAction(list.getSelectionModel().getSelectedItem(), false, list, hint); });
        dialog.showAndWait();
    }

    private void processFriendRequestAction(String requester, boolean accept, ListView<String> list, Label hint) {
        if (requester == null) return;
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                if (accept) apiService.acceptFriendRequest(username, requester);
                else        apiService.rejectFriendRequest(username, requester);
                Platform.runLater(() -> {
                    list.getItems().remove(requester);
                    if (list.getItems().isEmpty()) hint.setText("No pending friend requests.");
                    if (accept) { ensureFriendTracked(requester); renderFriendList(); }
                    loadFriends();
                });
            } catch (IOException e) { Platform.runLater(() -> showError("Failed: " + e.getMessage())); }
        });
    }

    private void showAddFriendDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Friends"); dialog.setHeaderText("Send requests or remove friends");
        TextField searchField = new TextField(); searchField.setPromptText("Search by username or name");
        ListView<String> results = new ListView<>(); results.setPrefHeight(180);
        Label hint = new Label("Type at least 2 characters to search.");
        List<String> currentFriends = new ArrayList<>(friendUsernames);
        currentFriends.sort(String::compareToIgnoreCase);
        ListView<String> currentList = new ListView<>(); currentList.setPrefHeight(150);
        currentList.getItems().addAll(currentFriends);
        Label removeHint = new Label(currentFriends.isEmpty() ? "No friends yet." : "Select and click Remove Friend.");

        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Add / Request Friends"), searchField, hint, results,
                new Separator(), new Label("Current Friends"), removeHint, currentList));

        ButtonType searchType = new ButtonType("Search");
        ButtonType sendType   = new ButtonType("Send Request");
        ButtonType removeType = new ButtonType("Remove Friend");
        dialog.getDialogPane().getButtonTypes().addAll(searchType, sendType, removeType, ButtonType.CLOSE);

        Button searchBtn = (Button) dialog.getDialogPane().lookupButton(searchType);
        Button sendBtn   = (Button) dialog.getDialogPane().lookupButton(sendType);
        Button removeBtn = (Button) dialog.getDialogPane().lookupButton(removeType);
        searchBtn.disableProperty().bind(searchField.textProperty().length().lessThan(2));
        sendBtn.disableProperty().bind(results.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.disableProperty().bind(currentList.getSelectionModel().selectedItemProperty().isNull());

        searchBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String q = searchField.getText().trim();
            if (q.length() < 2) { hint.setText("Type at least 2 characters."); return; }
            performUserSearch(q, results, hint);
        });
        sendBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String selected = results.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            String targetUsername = selected.split("\\s+")[0];
            sendFriendRequestToTarget(targetUsername, dialog);
        });
        removeBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String target = currentList.getSelectionModel().getSelectedItem();
            if (target == null) return;
            removeFriendTarget(target, currentList, removeHint);
        });
        dialog.showAndWait();
    }

    private void performUserSearch(String query, ListView<String> resultsList, Label hint) {
        String currentUser = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, Object>> res = apiService.searchUsers(query);
                Platform.runLater(() -> {
                    resultsList.getItems().clear();
                    for (Map<String, Object> user : res) {
                        Object u = user.get("username");
                        if (u == null || currentUser.equals(u.toString())) continue;
                        String fn = user.get("fname") != null ? user.get("fname").toString() : "";
                        String ln = user.get("lname") != null ? user.get("lname").toString() : "";
                        String full = (fn + " " + ln).trim();
                        resultsList.getItems().add(full.isBlank() ? u.toString() : u + "    " + full);
                    }
                    hint.setText(resultsList.getItems().isEmpty() ? "No users found." : "Select a user and send request.");
                });
            } catch (IOException e) { Platform.runLater(() -> hint.setText("Search failed: " + e.getMessage())); }
        });
    }

    private void sendFriendRequestToTarget(String targetUsername, Dialog<ButtonType> dialog) {
        String me = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.sendFriendRequest(me, targetUsername);
                Platform.runLater(() -> { dialog.close(); showInfo("Friend request sent to @" + targetUsername); loadFriends(); });
            } catch (IOException e) { Platform.runLater(() -> showError("Failed: " + e.getMessage())); }
        });
    }

    private void removeFriendTarget(String target, ListView<String> list, Label hint) {
        String me = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                apiService.removeFriend(me, target);
                Platform.runLater(() -> {
                    list.getItems().remove(target);
                    friendUsernames.remove(target);
                    friendUnread.remove(target); friendLastActivity.remove(target);
                    friendLastPreview.remove(target); friendTypingUntil.remove(target);
                    friendFullNames.remove(target);
                    if (target.equals(privateTarget)) {
                        privateTarget = null; userListView.getSelectionModel().clearSelection();
                        clearTypingIndicator(); updateChatHeader(); loadPublicHistory();
                        updateCallControls(); updateRoomControls();
                    }
                    renderFriendList();
                    if (list.getItems().isEmpty()) hint.setText("No friends to remove yet.");
                    showInfo("Removed @" + target);
                    loadFriends();
                });
            } catch (IOException e) { Platform.runLater(() -> showError("Failed: " + e.getMessage())); }
        });
    }

    private void showCreateRoomDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Group Room"); dialog.setHeaderText("Create a room and invite members");
        TextField roomNameField = new TextField(); roomNameField.setPromptText("Room name");
        ListView<String> members = new ListView<>();
        members.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        members.setPrefHeight(200);
        // Show full names in member picker
        for (String u : friendUsernames)
            members.getItems().add(friendFullNames.getOrDefault(u, u) + " (@" + u + ")");
        Label hint = new Label(friendUsernames.isEmpty() ? "No friends to invite yet." : "Optional: select friends.");
        dialog.getDialogPane().setContent(new VBox(10, new Label("Room Name"), roomNameField,
                new Label("Invite Members"), members, hint));
        ButtonType createType = new ButtonType("Create");
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);
        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createType);
        createBtn.disableProperty().bind(roomNameField.textProperty().isEmpty());
        createBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            String rn = roomNameField.getText().trim();
            if (rn.isBlank()) { hint.setText("Room name is required."); return; }
            // Extract usernames from display strings
            List<String> invited = new ArrayList<>();
            for (String sel : members.getSelectionModel().getSelectedItems()) {
                int at = sel.lastIndexOf("(@");
                if (at >= 0) invited.add(sel.substring(at + 2, sel.length() - 1));
            }
            createRoomWithInvites(rn, invited, dialog);
        });
        dialog.showAndWait();
    }

    private void createRoomWithInvites(String roomName, List<String> invited, Dialog<ButtonType> dialog) {
        String username = UserSession.getInstance().getUsername();
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> room = apiService.createRoom(roomName, "", username);
                Object idRaw = room.get("id");
                if (idRaw == null) throw new IOException("Room creation missing id.");
                long roomId = Long.parseLong(idRaw.toString());
                String roomIdText = Long.toString(roomId);
                for (String member : invited) {
                    if (member == null || member.isBlank() || username.equals(member)) continue;
                    try { apiService.joinRoom(roomId, member); } catch (IOException ignored) {}
                }
                Platform.runLater(() -> {
                    wsService.subscribeToRoom(roomIdText);
                    roomTarget = roomIdText; roomUnread.put(roomIdText, 0); privateTarget = null;
                    updateChatHeader(); dialog.close(); loadRooms(); loadRoomHistory(roomIdText);
                    updateCallControls(); updateRoomControls();
                    showInfo("Room created: " + roomName);
                });
            } catch (Exception ex) { Platform.runLater(() -> showError("Failed: " + ex.getMessage())); }
        });
    }

    private void showBlockedUsersDialog(List<Map<String, Object>> blocked) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Blocked Users"); dialog.setHeaderText("Manage blocked users");
        TextField usernameField = new TextField(); usernameField.setPromptText("Username to block");
        ListView<String> blockedList = new ListView<>(); blockedList.setPrefHeight(180);
        if (blocked != null) {
            for (Map<String, Object> item : blocked) {
                Object u = item.get("username"); if (u != null) blockedList.getItems().add(u.toString());
            }
        }
        Label hint = new Label("Select an entry to unblock.");
        dialog.getDialogPane().setContent(new VBox(10, new Label("Block a user"), usernameField, hint, blockedList));
        ButtonType blockType = new ButtonType("Block"), unblockType = new ButtonType("Unblock");
        dialog.getDialogPane().getButtonTypes().addAll(blockType, unblockType, ButtonType.CLOSE);
        Button blockBtn   = (Button) dialog.getDialogPane().lookupButton(blockType);
        Button unblockBtn = (Button) dialog.getDialogPane().lookupButton(unblockType);
        blockBtn.disableProperty().bind(usernameField.textProperty().isEmpty());
        unblockBtn.disableProperty().bind(blockedList.getSelectionModel().selectedItemProperty().isNull());
        blockBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume(); String target = usernameField.getText().trim(); if (target.isEmpty()) return;
            String u = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> { try { apiService.blockUser(u, target); Platform.runLater(() -> { if (!blockedList.getItems().contains(target)) blockedList.getItems().add(target); usernameField.clear(); }); } catch (IOException ex) { Platform.runLater(() -> showError("Failed: " + ex.getMessage())); } });
        });
        unblockBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume(); String target = blockedList.getSelectionModel().getSelectedItem(); if (target == null) return;
            String u = UserSession.getInstance().getUsername();
            CompletableFuture.runAsync(() -> { try { apiService.unblockUser(u, target); Platform.runLater(() -> blockedList.getItems().remove(target)); } catch (IOException ex) { Platform.runLater(() -> showError("Failed: " + ex.getMessage())); } });
        });
        dialog.showAndWait();
    }

    // ─── Status dialogs ───────────────────────────────────────────────────────

    private void showError(String message) {
        showStatusDialog("Error", message != null ? message : "An unexpected error occurred.", true);
    }

    private void showInfo(String message) {
        showStatusDialog("Notice", message != null ? message : "Done.", false);
    }

    private void showStatusDialog(String header, String message, boolean errorStyle) {
        Runnable task = () -> {
            Stage dialog = new Stage();
            dialog.setTitle("ChitChat");
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (chatRoot != null && chatRoot.getScene() != null)
                dialog.initOwner(chatRoot.getScene().getWindow());

            Text headerLabel = new Text(header);
            headerLabel.getStyleClass().add("status-dialog-header");
            Text msgLabel = new Text(message);
            msgLabel.getStyleClass().add("status-dialog-message");
            msgLabel.setWrappingWidth(380);

            Button ok = new Button("OK");
            ok.setDefaultButton(true);
            ok.setOnAction(e -> dialog.close());
            ok.getStyleClass().add("status-dialog-ok");
            if (errorStyle) ok.getStyleClass().add("status-dialog-ok-error");

            HBox actions = new HBox(ok);
            actions.setAlignment(Pos.CENTER_RIGHT);

            VBox root = new VBox(12, headerLabel, msgLabel, actions);
            root.setPadding(new Insets(16));
            root.getStyleClass().add("status-dialog-root");
            if (chatRoot != null)
                chatRoot.getStyleClass().stream()
                        .filter(c -> c.startsWith("pref-"))
                        .forEach(root.getStyleClass()::add);

            Scene scene = new Scene(root, 420, Region.USE_COMPUTED_SIZE);
            URL css = getClass().getResource("/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        };
        if (Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }
}
