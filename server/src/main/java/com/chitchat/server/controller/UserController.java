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

    // ── Friends ────────────────────────────────────────────────────────────────

    @GetMapping("/{username}/friends")
    public ResponseEntity<?> getFriends(@PathVariable String username) {
        List<UserEntity> friends = friendService.getFriends(username);
        List<Map<String, String>> result = friends.stream()
                .map(f -> Map.of("username", f.getUsername(), "fname", f.getFname(), "lname", f.getLname()))
                .toList();
        return ResponseEntity.ok(result);
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

    // ── Friend Requests ────────────────────────────────────────────────────────

    @GetMapping("/{username}/friend-requests")
    public ResponseEntity<?> getPendingRequests(@PathVariable String username) {
        List<UserEntity> pending = friendService.getPendingRequests(username);
        List<Map<String, String>> result = pending.stream()
                .map(u -> Map.of("username", u.getUsername(), "fname", u.getFname(), "lname", u.getLname()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{username}/friend-requests")
    public ResponseEntity<?> sendFriendRequest(@PathVariable String username, @RequestBody Map<String, String> body) {
        try {
            friendService.sendFriendRequest(username, body.get("targetUsername"));
            return ResponseEntity.ok(Map.of("message", "Friend request sent"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{username}/friend-requests/accept")
    public ResponseEntity<?> acceptFriendRequest(@PathVariable String username, @RequestBody Map<String, String> body) {
        try {
            friendService.acceptFriendRequest(username, body.get("requesterUsername"));
            return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{username}/friend-requests/reject")
    public ResponseEntity<?> rejectFriendRequest(@PathVariable String username, @RequestBody Map<String, String> body) {
        try {
            friendService.rejectFriendRequest(username, body.get("requesterUsername"));
            return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Blocked Users ──────────────────────────────────────────────────────────

    @GetMapping("/{username}/blocked")
    public ResponseEntity<?> getBlockedUsers(@PathVariable String username) {
        List<UserEntity> blocked = userService.getBlockedUsers(username);
        List<Map<String, String>> result = blocked.stream()
                .map(u -> Map.of("username", u.getUsername(), "fname", u.getFname(), "lname", u.getLname()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{username}/blocked")
    public ResponseEntity<?> blockUser(@PathVariable String username, @RequestBody Map<String, String> body) {
        try {
            userService.blockUser(username, body.get("targetUsername"));
            return ResponseEntity.ok(Map.of("message", "User blocked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{username}/blocked/{targetUsername}")
    public ResponseEntity<?> unblockUser(@PathVariable String username, @PathVariable String targetUsername) {
        try {
            userService.unblockUser(username, targetUsername);
            return ResponseEntity.ok(Map.of("message", "User unblocked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Preferences ────────────────────────────────────────────────────────────

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
        return ResponseEntity.ok(Map.of("message", "Preferences updated"));
    }
}