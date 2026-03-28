package com.chitchat.server.controller;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.chitchat.server.service.ChatMessageService;
import com.chitchat.server.service.UserService;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;

/**
 * Handles all real-time messaging over STOMP WebSocket.
 *
 * This controller processes every @MessageMapping endpoint — join/leave events,
 * public and private messages, group room messages, read receipts, typing indicators,
 * and WebRTC call signalling. It deliberately does no persistence itself; that's
 * delegated to ChatMessageService so the routing logic stays clean and readable.
 *
 * The two in-memory maps below are the only "state" this controller holds.
 * If the server restarts, both maps are cleared — which is fine since any connected
 * clients will reconnect and re-register themselves via /chat.join.
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    // Tracks which users are currently connected: username -> WebSocket sessionId.
    // ConcurrentHashMap because join/leave events can fire from any thread.
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();

    // Tracks who sent each message so we can route read receipts back to the right person.
    // Key = message UUID, value = sender's username.
    private final Map<String, String> messageSenders = new ConcurrentHashMap<>();

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageService chatMessageService,
                          UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageService = chatMessageService;
        this.userService = userService;
    }

    // ───── Presence ─────────────────────────────────────────────────────────

    // When a user connects, we store their session ID so we can associate it with
    // their username on disconnect. We also broadcast the updated online user list
    // to all subscribers so everyone's sidebar reflects the new arrival.
    @MessageMapping("/chat.join")
    public void joinChat(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        String username = message.getSender();
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs != null) attrs.put("username", username);
        onlineUsers.put(username, headerAccessor.getSessionId());

        broadcastUserList();
        Message joinMsg = new Message(MessageType.PUBLIC_MESSAGE, "System", null,
                username + " has joined the chat");
        messagingTemplate.convertAndSend("/topic/public", (Object) joinMsg);
    }

    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload Message message) {
        String username = message.getSender();
        onlineUsers.remove(username);
        broadcastUserList();
        Message leaveMsg = new Message(MessageType.PUBLIC_MESSAGE, "System", null,
                username + " has left the chat");
        messagingTemplate.convertAndSend("/topic/public", (Object) leaveMsg);
    }

    // ───── Messaging ─────────────────────────────────────────────────────────

    // We assign the UUID here on the server side so clients can't forge message IDs.
    // The ID is what the client sends back later in a /chat.read receipt.
    @MessageMapping("/chat.sendPublic")
    public void sendPublicMessage(@Payload Message message) {
        message.setId(UUID.randomUUID().toString());
        messageSenders.put(message.getId(), message.getSender());
        chatMessageService.save(message);
        messagingTemplate.convertAndSend("/topic/public", (Object) message);
    }

    // Private messages are sent to both the receiver AND the sender.
    // The sender copy ensures the message shows up in their own chat window immediately.
    // We also send on the /topic/user.* path as a fallback for clients that prefer
    // topic subscriptions over user-queue subscriptions (the JavaFX client does both).
    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload Message message) {
        String receiver = message.getReceiver();
        String sender = message.getSender();
        if (receiver == null || sender == null) return;
        // Drop the message if the receiver has blocked the sender.
        if (userService.isBlocked(receiver, sender)) return;
        message.setId(UUID.randomUUID().toString());
        messageSenders.put(message.getId(), sender);
        chatMessageService.save(message);
        messagingTemplate.convertAndSendToUser(receiver, "/queue/private", (Object) message);
        messagingTemplate.convertAndSendToUser(sender, "/queue/private", (Object) message);
        // Compatibility path for clients using topic-per-user subscriptions.
        messagingTemplate.convertAndSend("/topic/user." + receiver, (Object) message);
        messagingTemplate.convertAndSend("/topic/user." + sender, (Object) message);
    }

    @MessageMapping("/chat.sendRoom")
    public void sendRoomMessage(@Payload Message message) {
        String roomId = message.getRoomId();
        if (roomId == null) return;
        message.setId(UUID.randomUUID().toString());
        messageSenders.put(message.getId(), message.getSender());
        chatMessageService.save(message);
        // Broadcast to everyone subscribed to this room's topic.
        messagingTemplate.convertAndSend("/topic/room/" + roomId, (Object) message);
    }

    // ───── Read Receipts ─────────────────────────────────────────────────────

    // The client sends a /chat.read message containing the ID of the message it just displayed.
    // We look up who originally sent that message and notify them with a READ_RECEIPT.
    // We skip the receipt if the reader is the original sender (you don't need to be told you
    // read your own message).
    @MessageMapping("/chat.read")
    public void markRead(@Payload Message message) {
        String originalMsgId = message.getId();
        String reader = message.getSender();
        String originalSender = messageSenders.get(originalMsgId);
        if (originalSender == null || originalSender.equals(reader)) return;

        Message receipt = new Message(MessageType.READ_RECEIPT, reader, originalSender, originalMsgId);
        messagingTemplate.convertAndSendToUser(originalSender, "/queue/receipts", (Object) receipt);
        messagingTemplate.convertAndSend("/topic/receipts." + originalSender, (Object) receipt);
        messagingTemplate.convertAndSend("/topic/user." + originalSender, (Object) receipt);
    }

    // ───── Voice / Video Call Signalling ─────────────────────────────────────

    // These four handlers relay WebRTC signalling messages between peers.
    // The server doesn't interpret the content (SDP offers/answers, ICE candidates) —
    // it just forwards them to the target user. The actual peer connection is established
    // directly between the two browsers/clients via WebRTC.

    @MessageMapping("/call.offer")
    public void callOffer(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSend("/topic/user." + target, (Object) message);
    }

    @MessageMapping("/call.answer")
    public void callAnswer(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSend("/topic/user." + target, (Object) message);
    }

    @MessageMapping("/call.ice")
    public void callIce(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSend("/topic/user." + target, (Object) message);
    }

    @MessageMapping("/call.end")
    public void callEnd(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSend("/topic/user." + target, (Object) message);
    }

    // ───── Typing Indicator ──────────────────────────────────────────────────

    // Typing events are not persisted — they're purely ephemeral signals.
    // The client shows "X is typing..." for a few seconds then clears it.
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Message message) {
        String receiver = message.getReceiver();
        if (receiver == null) return;
        messagingTemplate.convertAndSendToUser(receiver, "/queue/private", (Object) message);
        messagingTemplate.convertAndSend("/topic/user." + receiver, (Object) message);
    }

    // ───── Disconnect ────────────────────────────────────────────────────────

    // Fires when a WebSocket session closes (tab closed, network drop, etc.).
    // We stored the username in the session attributes on join, so we can retrieve
    // it here even though we don't get a /chat.leave message on a hard disconnect.
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs == null) return;
        String username = (String) attrs.get("username");
        if (username == null) return;
        onlineUsers.remove(username);
        broadcastUserList();
        // Also update the DB so the user's "last seen" timestamp is accurate.
        userService.logout(username);
    }

    // ───── Helpers ───────────────────────────────────────────────────────────

    // Sends the current list of online usernames (comma-separated) to all clients.
    // Every client uses this to update the green dot / offline indicators in the sidebar.
    private void broadcastUserList() {
        Message msg = new Message(MessageType.USER_LIST, "Server", null,
                String.join(",", onlineUsers.keySet()));
        messagingTemplate.convertAndSend("/topic/users", (Object) msg);
    }
}
