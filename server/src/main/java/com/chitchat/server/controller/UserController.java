package com.chitchat.server.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.service.FriendService;
import com.chitchat.server.service.UserService;
import com.chitchat.shared.UserPreferences;
import java.util.LinkedHashMap;

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

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String, String> userDto(UserEntity u) {
        var m = new LinkedHashMap<String, String>();
        m.put("username", u.getUsername());
        m.put("fname", u.getFname());
        m.put("lname", u.getLname());
        m.put("avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
        return m;
    }

    // ── Friends ────────────────────────────────────────────────────────────────

    @GetMapping("/{username}/friends")
    public ResponseEntity<?> getFriends(@PathVariable String username) {
        List<Map<String, String>> result = friendService.getFriends(username).stream()
                .map(this::userDto).toList();
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
        List<Map<String, String>> result = friendService.getPendingRequests(username).stream()
                .map(this::userDto).toList();
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
        List<Map<String, String>> result = userService.getBlockedUsers(username).stream()
                .map(this::userDto).toList();
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

    // ── Avatar ─────────────────────────────────────────────────────────────────

    @PutMapping("/{username}/avatar")
    public ResponseEntity<?> updateAvatar(@PathVariable String username, @RequestBody Map<String, String> body) {
        UserEntity user = userService.findByUsername(username);
        user.setAvatarUrl(body.get("avatarUrl"));
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Avatar updated"));
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        List<Map<String, String>> result = userService.searchUsers(q).stream()
                .map(this::userDto).toList();
        return ResponseEntity.ok(result);
    }

    // ── Profile ─────────────────────────────────────────────────────────────────

    @GetMapping("/{username}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        UserEntity user = userService.findByUsername(username);
        UserPreferences prefs = user.getPreferences() != null ? user.getPreferences() : new UserPreferences();
        var m = new LinkedHashMap<String, String>();
        m.put("username", user.getUsername());
        m.put("fname", user.getFname());
        m.put("lname", user.getLname());
        m.put("lastSeen", user.getLastOnline() != null ? user.getLastOnline() : "");
        m.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        m.put("status", prefs.getStatus() != null ? prefs.getStatus() : "Online");
        m.put("bio", prefs.getBio() != null ? prefs.getBio() : "");
        return ResponseEntity.ok(m);
    }

    @PutMapping("/{username}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable String username,
                                           @RequestBody Map<String, String> body) {
        UserEntity user = userService.findByUsername(username);
        UserPreferences prefs = user.getPreferences() != null ? user.getPreferences() : new UserPreferences();
        prefs.setStatus(body.getOrDefault("status", "Online"));
        prefs.setBio(body.getOrDefault("bio", ""));
        user.setPreferences(prefs);
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
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
