package com.chitchat.client.service;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.chitchat.shared.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class WebSocketService {

    private WebSocketClient wsClient;
    private final ObjectMapper mapper;
    private Consumer<Message> onMessage;
    private Consumer<String> onUserList;
    private final Set<String> roomSubscriptions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chitchat-ws-reconnect");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile boolean manualDisconnect = false;
    private volatile String lastServerUrl;
    private volatile String lastUsername;

    public WebSocketService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void setOnMessage(Consumer<Message> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnUserList(Consumer<String> onUserList) {
        this.onUserList = onUserList;
    }

    /**
     * Connect to the STOMP-over-WebSocket endpoint using a raw WebSocket.
     * We manually implement the STOMP handshake here to avoid pulling in
     * a full STOMP library dependency.
     */
    public void connect(String serverUrl, String username) {
        lastServerUrl = serverUrl;
        lastUsername = username;
        manualDisconnect = false;

        openConnection(serverUrl, username);
    }

    private synchronized void openConnection(String serverUrl, String username) {
        if (wsClient != null && wsClient.isOpen()) {
            return;
        }
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception ignored) {
                // Reconnect flow should continue even if cleanup fails.
            }
            wsClient = null;
        }

        try {
            // SockJS info endpoint returns a raw WS URL like ws://host/ws/{serverid}/{sessionid}/websocket
            String wsUrl = serverUrl
                    .replace("http://", "ws://")
                    .replace("https://", "wss://")
                    + "/ws/websocket";

            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    // Send STOMP CONNECT frame with username so server can route messages
                    send("CONNECT\naccept-version:1.2\nheart-beat:0,0\nlogin:" + username + "\n\n\u0000");
                }

                @Override
                public void onMessage(String text) {
                    handleFrame(text, username);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket error: " + ex.getMessage());
                    scheduleReconnect();
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (manualDisconnect || lastServerUrl == null || lastUsername == null) {
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        int attempt = reconnectAttempts.incrementAndGet();
        long delaySeconds = Math.min(30, 1L << Math.min(attempt - 1, 5));
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            if (manualDisconnect) {
                return;
            }
            openConnection(lastServerUrl, lastUsername);
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void handleFrame(String frame, String username) {
        if (frame.startsWith("CONNECTED")) {
            reconnectAttempts.set(0);
            // STOMP connected — subscribe and join
            wsClient.send("SUBSCRIBE\nid:sub-public\ndestination:/topic/public\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-private\ndestination:/user/queue/private\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-user-topic\ndestination:/topic/user." + username + "\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-users\ndestination:/topic/users\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-receipts\ndestination:/user/queue/receipts\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-call\ndestination:/user/queue/call\n\n\u0000");
            for (String roomId : roomSubscriptions) {
                subscribeToRoom(roomId);
            }
            // Send join message
            sendMessage(new com.chitchat.shared.Message(
                    com.chitchat.shared.MessageType.LOGIN, username, null, ""));
        } else if (frame.startsWith("MESSAGE")) {
            // Extract body (after double newline, before null byte)
            int bodyStart = frame.indexOf("\n\n");
            if (bodyStart < 0) return;
            String body = frame.substring(bodyStart + 2).replace("\u0000", "").trim();
            if (body.isEmpty()) return;

            try {
                Message msg = mapper.readValue(body, Message.class);
                if (msg.getType() == com.chitchat.shared.MessageType.USER_LIST) {
                    if (onUserList != null) onUserList.accept(msg.getContent());
                } else {
                    if (onMessage != null) onMessage.accept(msg);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse message: " + e.getMessage());
            }
        }
    }

    public void sendMessage(Message message) {
        if (wsClient == null || !wsClient.isOpen()) return;
        try {
            String destination = message.getReceiver() != null
                    ? "/app/chat.sendPrivate"
                    : "/app/chat.sendPublic";
            if (message.getType() == com.chitchat.shared.MessageType.LOGIN) {
                destination = "/app/chat.join";
            } else if (message.getType() == com.chitchat.shared.MessageType.LOGOUT) {
                destination = "/app/chat.leave";
            } else if (message.getType() == com.chitchat.shared.MessageType.ROOM_MESSAGE) {
                destination = "/app/chat.sendRoom";
            } else if (message.getType() == com.chitchat.shared.MessageType.READ_RECEIPT) {
                destination = "/app/chat.read";
            } else if (message.getType() == com.chitchat.shared.MessageType.TYPING) {
                destination = "/app/chat.typing";
            } else if (message.getType() == com.chitchat.shared.MessageType.CALL_OFFER) {
                destination = "/app/call.offer";
            } else if (message.getType() == com.chitchat.shared.MessageType.CALL_ANSWER) {
                destination = "/app/call.answer";
            } else if (message.getType() == com.chitchat.shared.MessageType.CALL_ICE) {
                destination = "/app/call.ice";
            } else if (message.getType() == com.chitchat.shared.MessageType.CALL_END
                    || message.getType() == com.chitchat.shared.MessageType.CALL_REJECT) {
                destination = "/app/call.end";
            }
            String json = mapper.writeValueAsString(message);
            String frame = "SEND\ndestination:" + destination + "\ncontent-type:application/json\n\n"
                    + json + "\u0000";
            wsClient.send(frame);
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    public void disconnect(String username) {
        manualDisconnect = true;
        sendMessage(new Message(com.chitchat.shared.MessageType.LOGOUT, username, null, ""));
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send("DISCONNECT\n\n\u0000");
            wsClient.close();
        }
    }

    public void subscribeToRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) return;
        roomSubscriptions.add(roomId);
        if (wsClient == null || !wsClient.isOpen()) return;
        String frame = "SUBSCRIBE\nid:sub-room-" + roomId + "\ndestination:/topic/room/" + roomId + "\n\n\u0000";
        wsClient.send(frame);
    }
}
