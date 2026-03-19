package com.chitchat.server.service;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new IllegalArgumentException("Password must contain at least one symbol.");
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

    public void blockUser(String username, String targetUsername) {
        UserEntity user = findByUsername(username);
        UserEntity target = findByUsername(targetUsername);
        if (!user.getBlockedList().contains(target)) {
            user.getBlockedList().add(target);
            userRepository.save(user);
        }
    }

    public void unblockUser(String username, String targetUsername) {
        UserEntity user = findByUsername(username);
        UserEntity target = findByUsername(targetUsername);
        user.getBlockedList().remove(target);
        userRepository.save(user);
    }

    public List<UserEntity> getBlockedUsers(String username) {
        return findByUsername(username).getBlockedList();
    }
}
