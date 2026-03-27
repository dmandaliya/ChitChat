package com.chitchat.server.controller;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.service.JwtUtil;
import com.chitchat.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            UserEntity user = userService.login(body.get("username"), body.get("password"));
            return ResponseEntity.ok(toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        userService.logout(body.get("username"));
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    private Map<String, Object> toDto(UserEntity user) {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("id", user.getId());
        map.put("fname", user.getFname());
        map.put("lname", user.getLname());
        map.put("username", user.getUsername());
        map.put("lastOnline", user.getLastOnline() != null ? user.getLastOnline() : "");
        map.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        map.put("token", jwtUtil.generateToken(user.getUsername()));
        return map;
    }
}
