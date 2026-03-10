package com.chitchat.server.service;

import com.chitchat.server.model.UserEntity;
import com.chitchat.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FriendService {

    private final UserRepository userRepository;

    public FriendService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void addFriend(String username, String friendUsername) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        UserEntity friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + friendUsername));

        if (!user.getFriendList().contains(friend)) {
            user.getFriendList().add(friend);
            userRepository.save(user);
        }
    }

    public void removeFriend(String username, String friendUsername) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        UserEntity friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + friendUsername));

        user.getFriendList().remove(friend);
        userRepository.save(user);
    }

    public List<UserEntity> getFriends(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getFriendList();
    }
}
