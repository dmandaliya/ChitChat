package com.chitchat.server.controller;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.server.model.MessageReaction;
import com.chitchat.server.repository.MessageReactionRepository;
import com.chitchat.server.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageHistoryController {

    private final ChatMessageService chatMessageService;
    private final MessageReactionRepository reactionRepository;

    public MessageHistoryController(ChatMessageService chatMessageService,
                                    MessageReactionRepository reactionRepository) {
        this.chatMessageService = chatMessageService;
        this.reactionRepository = reactionRepository;
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<?> addReaction(@PathVariable String id,
                                         @RequestBody Map<String, String> body) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(id);
        reaction.setUsername(body.get("username"));
        reaction.setEmoji(body.get("emoji"));
        reactionRepository.save(reaction);
        return ResponseEntity.ok(Map.of("message", "Reaction added"));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id,
                                           @RequestParam String username) {
        try {
            chatMessageService.deleteMessage(id, username);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
