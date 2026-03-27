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

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    // username -> sessionId
    private final Map<String, String> onlineUsers = new ConcurrentHashMap<>();

    // messageId -> sender (for read receipts)
    private final Map<String, String> messageSenders = new ConcurrentHashMap<>();

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageService chatMessageService,
                          UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageService = chatMessageService;
        this.userService = userService;
    }

    // ───── Presence ─────────────────────────────────────────────────────────

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

    @MessageMapping("/chat.sendPublic")
    public void sendPublicMessage(@Payload Message message) {
        message.setId(UUID.randomUUID().toString());
        messageSenders.put(message.getId(), message.getSender());
        chatMessageService.save(message);
        messagingTemplate.convertAndSend("/topic/public", (Object) message);
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload Message message) {
        String receiver = message.getReceiver();
        String sender = message.getSender();
        if (receiver == null || sender == null) return;
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
        messagingTemplate.convertAndSend("/topic/room/" + roomId, (Object) message);
    }

    // ───── Read Receipts ─────────────────────────────────────────────────────

    @MessageMapping("/chat.read")
    public void markRead(@Payload Message message) {
        String originalMsgId = message.getId();
        String reader = message.getSender();
        String originalSender = messageSenders.get(originalMsgId);
        if (originalSender == null || originalSender.equals(reader)) return;

        Message receipt = new Message(MessageType.READ_RECEIPT, reader, originalSender, originalMsgId);
        messagingTemplate.convertAndSendToUser(originalSender, "/queue/receipts", (Object) receipt);
        messagingTemplate.convertAndSend("/topic/user." + originalSender, (Object) receipt);
    }

    // ───── Voice / Video Call Signalling ─────────────────────────────────────

    @MessageMapping("/call.offer")
    public void callOffer(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSendToUser(target, "/queue/call", (Object) message);
    }

    @MessageMapping("/call.answer")
    public void callAnswer(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSendToUser(target, "/queue/call", (Object) message);
    }

    @MessageMapping("/call.ice")
    public void callIce(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSendToUser(target, "/queue/call", (Object) message);
    }

    @MessageMapping("/call.end")
    public void callEnd(@Payload Message message) {
        String target = message.getReceiver();
        if (target == null) return;
        messagingTemplate.convertAndSendToUser(target, "/queue/call", (Object) message);
    }

    // ───── Typing Indicator ──────────────────────────────────────────────────

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Message message) {
        String receiver = message.getReceiver();
        if (receiver == null) return;
        messagingTemplate.convertAndSendToUser(receiver, "/queue/private", (Object) message);
        messagingTemplate.convertAndSend("/topic/user." + receiver, (Object) message);
    }

    // ───── Disconnect ────────────────────────────────────────────────────────

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs == null) return;
        String username = (String) attrs.get("username");
        if (username == null) return;
        onlineUsers.remove(username);
        broadcastUserList();
        userService.logout(username);
    }

    // ───── Helpers ───────────────────────────────────────────────────────────

    private void broadcastUserList() {
        Message msg = new Message(MessageType.USER_LIST, "Server", null,
                String.join(",", onlineUsers.keySet()));
        messagingTemplate.convertAndSend("/topic/users", (Object) msg);
    }
}
