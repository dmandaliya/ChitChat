package com.chitchat.server.controller;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles user registration, login, and logout over HTTP.
 *
 * All three endpoints live under /api/auth. We deliberately keep this controller
 * thin — actual validation and BCrypt hashing happen in UserService so the logic
 * is testable without spinning up the web layer.
 *
 * @CrossOrigin is set to "*" so the web frontend can hit these endpoints from
 * any origin during development. In a production environment you'd lock this down
 * to the actual domain.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Registers a new user. Returns 400 with an error message if the username
    // is taken or the password doesn't meet our complexity rules.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            UserEntity user = userService.register(
                    body.get("fname"),
                    body.get("lname"),
                    body.get("username"),
                    body.get("password")
            );
            return ResponseEntity.ok(toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Logs in an existing user. Returns 401 if credentials are wrong.
    // On success, marks the user as loggedIn and updates their lastOnline timestamp.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            UserEntity user = userService.login(body.get("username"), body.get("password"));
            return ResponseEntity.ok(toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // Logs out a user — just updates their loggedIn flag and lastOnline in the DB.
    // The client is responsible for closing its own WebSocket connection separately.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        userService.logout(body.get("username"));
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // We intentionally strip the hashed password before sending user data back to
    // the client. The client only ever needs display info (name, username, lastOnline).
    private Map<String, Object> toDto(UserEntity user) {
        return Map.of(
                "id", user.getId(),
                "fname", user.getFname(),
                "lname", user.getLname(),
                "username", user.getUsername(),
                "lastOnline", user.getLastOnline() != null ? user.getLastOnline() : ""
        );
    }
}
