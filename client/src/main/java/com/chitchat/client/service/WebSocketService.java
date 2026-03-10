package com.chitchat.client.service;

import com.chitchat.shared.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class WebSocketService {

    private WebSocketClient wsClient;
    private final ObjectMapper mapper;
    private Consumer<Message> onMessage;
    private Consumer<String> onUserList;

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
        try {
            // SockJS info endpoint returns a raw WS URL like ws://host/ws/{serverid}/{sessionid}/websocket
            String wsUrl = serverUrl
                    .replace("http://", "ws://")
                    .replace("https://", "wss://")
                    + "/ws/websocket";

            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    // Send STOMP CONNECT frame
                    send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000");
                }

                @Override
                public void onMessage(String text) {
                    handleFrame(text, username);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket error: " + ex.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect WebSocket", e);
        }
    }

    private void handleFrame(String frame, String username) {
        if (frame.startsWith("CONNECTED")) {
            // STOMP connected — subscribe and join
            wsClient.send("SUBSCRIBE\nid:sub-public\ndestination:/topic/public\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-private\ndestination=/user/queue/private\n\n\u0000");
            wsClient.send("SUBSCRIBE\nid:sub-users\ndestination:/topic/users\n\n\u0000");
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
        sendMessage(new Message(com.chitchat.shared.MessageType.LOGOUT, username, null, ""));
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send("DISCONNECT\n\n\u0000");
            wsClient.close();
        }
    }
}
