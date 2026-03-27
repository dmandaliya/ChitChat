package com.chitchat.server.controller;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.server.model.MessageReaction;
import com.chitchat.server.repository.MessageReactionRepository;
import com.chitchat.server.service.ChatMessageService;
import com.chitchat.server.service.RateLimitService;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageHistoryController {

    private final ChatMessageService chatMessageService;
    private final MessageReactionRepository reactionRepository;
    private final RateLimitService rateLimitService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageHistoryController(ChatMessageService chatMessageService,
                                    MessageReactionRepository reactionRepository,
                                    RateLimitService rateLimitService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.reactionRepository = reactionRepository;
        this.rateLimitService = rateLimitService;
        this.messagingTemplate = messagingTemplate;
    }

    // ───── Reactions ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/react")
    public ResponseEntity<?> addReaction(@PathVariable String id,
                                         @RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (!rateLimitService.isAllowed(username)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. Try again later."));
        }
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(id);
        reaction.setUsername(username);
        reaction.setEmoji(body.get("emoji"));
        reactionRepository.save(reaction);
        return ResponseEntity.ok(Map.of("message", "Reaction added"));
    }

    // ───── History ───────────────────────────────────────────────────────────

    @GetMapping("/public")
    public List<ChatMessage> getPublic() {
        return chatMessageService.getPublicHistory();
    }

    @GetMapping("/private")
    public List<ChatMessage> getPrivate(@RequestParam String user1, @RequestParam String user2) {
        return chatMessageService.getPrivateHistory(user1, user2);
    }

    @GetMapping("/room/{roomId}")
    public List<ChatMessage> getRoom(@PathVariable String roomId) {
        return chatMessageService.getRoomHistory(roomId);
    }

    // ───── Search ────────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String user1,
                                    @RequestParam String user2,
                                    @RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search keyword cannot be empty"));
        }
        return ResponseEntity.ok(chatMessageService.searchPrivateMessages(user1, user2, q));
    }

    // ───── Edit ──────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> editMessage(@PathVariable String id,
                                         @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String newContent = body.get("content");
        if (!rateLimitService.isAllowed(username)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. Try again later."));
        }
        ChatMessage updated = chatMessageService.editMessage(id, newContent, username);

        // Broadcast edit via WebSocket so clients update in real-time
        Message wsMsg = new Message(MessageType.MESSAGE_EDITED, updated.getSender(),
                updated.getReceiver(), updated.getContent());
        wsMsg.setId(updated.getId());
        wsMsg.setEdited(true);
        if (updated.getReceiver() != null) {
            messagingTemplate.convertAndSend("/topic/user." + updated.getReceiver(), (Object) wsMsg);
            messagingTemplate.convertAndSend("/topic/user." + updated.getSender(), (Object) wsMsg);
        } else if (updated.getRoomId() != null) {
            messagingTemplate.convertAndSend("/topic/room/" + updated.getRoomId(), (Object) wsMsg);
        } else {
            messagingTemplate.convertAndSend("/topic/public", (Object) wsMsg);
        }
        return ResponseEntity.ok(updated);
    }

    // ───── Delete ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id,
                                           @RequestParam String username) {
        if (!rateLimitService.isAllowed(username)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. Try again later."));
        }
        ChatMessage updated = chatMessageService.deleteMessage(id, username);

        // Broadcast delete via WebSocket
        Message wsMsg = new Message(MessageType.MESSAGE_DELETED, updated.getSender(),
                updated.getReceiver(), updated.getId());
        wsMsg.setId(updated.getId());
        wsMsg.setDeleted(true);
        if (updated.getReceiver() != null) {
            messagingTemplate.convertAndSend("/topic/user." + updated.getReceiver(), (Object) wsMsg);
            messagingTemplate.convertAndSend("/topic/user." + updated.getSender(), (Object) wsMsg);
        } else if (updated.getRoomId() != null) {
            messagingTemplate.convertAndSend("/topic/room/" + updated.getRoomId(), (Object) wsMsg);
        } else {
            messagingTemplate.convertAndSend("/topic/public", (Object) wsMsg);
        }
        return ResponseEntity.ok(Map.of("message", "Message deleted"));
    }
}
