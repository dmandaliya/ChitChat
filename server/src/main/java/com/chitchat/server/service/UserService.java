package com.chitchat.server.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.repository.UserRepository;

/**
 * Core business logic for user accounts — registration, login, logout, and blocking.
 *
 * We keep all password-related logic here (validation + hashing) rather than in the
 * controller so it stays easy to test and doesn't leak into the HTTP layer.
 * Passwords are never stored as plaintext; BCrypt hashing is handled by EncryptionService.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public UserService(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Creates a new user account after validating the username and password.
     * Throws IllegalArgumentException (which the controller converts to a 400) if:
     *  - the username is already taken
     *  - the password is under 8 characters
     *  - the password has no uppercase letter
     *  - the password has no special character (anything that isn't a letter or digit)
     */
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
        // Hash the password before touching the DB — plaintext never gets persisted.
        UserEntity user = new UserEntity(fname, lname, username, encryptionService.hashPassword(password));
        return userRepository.save(user);
    }

    /**
     * Verifies credentials and marks the user as logged in.
     * We store lastOnline as an ISO string rather than a proper datetime column
     * to keep the schema simple and avoid timezone headaches across MySQL/PostgreSQL.
     */
    public UserEntity login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // BCrypt handles the hash comparison internally — we never decode the stored hash.
        if (!encryptionService.matches(password, user.getHashedPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        user.setLoggedIn(true);
        user.setLastOnline(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return userRepository.save(user);
    }

    // Called both from the REST logout endpoint and from the WebSocket disconnect listener
    // so the user's presence state stays accurate even if they close the app without logging out.
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

    // We guard against double-blocking so the blocked list stays clean.
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

    // Delegates to a JPQL LIKE query that searches both username and display name
    // (case-insensitive) — see UserRepository.searchByUsernameOrName.
    public List<UserEntity> searchUsers(String q) {
        return userRepository.searchByUsernameOrName(q);
    }

    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }
}
