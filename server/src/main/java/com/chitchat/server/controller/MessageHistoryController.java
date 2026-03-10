package com.chitchat.server.controller;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.server.service.ChatMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageHistoryController {

    private final ChatMessageService chatMessageService;

    public MessageHistoryController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
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
}
