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

    // Sends a friend request — mirrors srs/FriendService.addFriend()
    // Adds sender to target's pendingRequests (does NOT make them friends yet).
    public void sendFriendRequest(String senderUsername, String targetUsername) {
        UserEntity sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + senderUsername));
        UserEntity target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUsername));
        if (!target.getPendingRequests().contains(sender) && !sender.getFriendList().contains(target)) {
            target.getPendingRequests().add(sender);
            userRepository.save(target);
        }
    }

    // Accepts an incoming request — mirrors srs/FriendService.acceptRequest()
    // Removes requester from pendingRequests, adds both users to each other's friendList.
    public void acceptFriendRequest(String acceptorUsername, String requesterUsername) {
        UserEntity acceptor = userRepository.findByUsername(acceptorUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + acceptorUsername));
        UserEntity requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requesterUsername));
        if (!acceptor.getPendingRequests().contains(requester)) {
            throw new IllegalArgumentException("No pending request from: " + requesterUsername);
        }
        acceptor.getPendingRequests().remove(requester);
        if (!acceptor.getFriendList().contains(requester)) acceptor.getFriendList().add(requester);
        if (!requester.getFriendList().contains(acceptor)) requester.getFriendList().add(acceptor);
        userRepository.save(acceptor);
        userRepository.save(requester);
    }

    // Rejects an incoming request — mirrors srs/FriendService.rejectRequest()
    public void rejectFriendRequest(String rejectorUsername, String requesterUsername) {
        UserEntity rejector = userRepository.findByUsername(rejectorUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + rejectorUsername));
        UserEntity requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requesterUsername));
        rejector.getPendingRequests().remove(requester);
        userRepository.save(rejector);
    }

    // Removes friend from both sides — mirrors srs/FriendService.removeFriend()
    public void removeFriend(String username, String friendUsername) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        UserEntity friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + friendUsername));
        user.getFriendList().remove(friend);
        friend.getFriendList().remove(user);
        userRepository.save(user);
        userRepository.save(friend);
    }

    public List<UserEntity> getFriends(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getFriendList();
    }

    public List<UserEntity> getPendingRequests(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                .getPendingRequests();
    }
}