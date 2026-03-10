package com.chitchat.server.service;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public UserService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public UserEntity register(String fname, String lname, String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        UserEntity user = new UserEntity(fname, lname, username, encryptionService.hashPassword(password));
        return userRepository.save(user);
    }

    public UserEntity login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!encryptionService.matches(password, user.getHashedPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        user.setLoggedIn(true);
        user.setLastOnline(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return userRepository.save(user);
    }

    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLoggedIn(false);
            user.setLastOnline(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            userRepository.save(user);
        });
    }

    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
