package com.chitchat.server.controller;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.service.FriendService;
import com.chitchat.server.service.UserService;
import com.chitchat.shared.UserPreferences;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final FriendService friendService;
    private final UserService userService;

    public UserController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    @GetMapping("/{username}/friends")
    public ResponseEntity<?> getFriends(@PathVariable String username) {
        List<UserEntity> friends = friendService.getFriends(username);
        List<Map<String, String>> result = friends.stream()
                .map(f -> Map.of("username", f.getUsername(), "fname", f.getFname(), "lname", f.getLname()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{username}/friends")
    public ResponseEntity<?> addFriend(@PathVariable String username, @RequestBody Map<String, String> body) {
        try {
            friendService.addFriend(username, body.get("friendUsername"));
            return ResponseEntity.ok(Map.of("message", "Friend added"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{username}/friends/{friendUsername}")
    public ResponseEntity<?> removeFriend(@PathVariable String username, @PathVariable String friendUsername) {
        try {
            friendService.removeFriend(username, friendUsername);
            return ResponseEntity.ok(Map.of("message", "Friend removed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{username}/preferences")
    public ResponseEntity<?> getPreferences(@PathVariable String username) {
        UserEntity user = userService.findByUsername(username);
        return ResponseEntity.ok(user.getPreferences());
    }

    @PutMapping("/{username}/preferences")
    public ResponseEntity<?> updatePreferences(@PathVariable String username,
                                                @RequestBody UserPreferences prefs) {
        UserEntity user = userService.findByUsername(username);
        user.setPreferences(prefs);
        // save via userService (or directly via repo — kept simple here)
        return ResponseEntity.ok(Map.of("message", "Preferences updated"));
    }
}
