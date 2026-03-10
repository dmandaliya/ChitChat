package com.chitchat.server.controller;

import com.chitchat.server.model.ChatRoom;
import com.chitchat.server.service.ChatRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRooms() {
        return ResponseEntity.ok(chatRoomService.getAllRooms().stream().map(this::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> body) {
        try {
            ChatRoom room = chatRoomService.createRoom(
                    body.get("name"), body.get("description"), body.get("username"));
            return ResponseEntity.ok(toDto(room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable Long roomId, @RequestBody Map<String, String> body) {
        try {
            ChatRoom room = chatRoomService.joinRoom(roomId, body.get("username"));
            return ResponseEntity.ok(toDto(room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId, @RequestBody Map<String, String> body) {
        try {
            chatRoomService.leaveRoom(roomId, body.get("username"));
            return ResponseEntity.ok(Map.of("message", "Left room"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toDto(ChatRoom room) {
        List<String> memberNames = room.getMembers().stream()
                .map(m -> m.getUsername()).toList();
        return Map.of(
                "id", room.getId(),
                "name", room.getName(),
                "description", room.getDescription() != null ? room.getDescription() : "",
                "createdBy", room.getCreatedBy(),
                "members", memberNames
        );
    }
}
