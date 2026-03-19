package com.chitchat.shared;

import java.util.List;

public class FriendService {
    private User user;
    private List<User> friends;

    public FriendService(User user) {
        this.user = user;
        this.friends = user.getFriendList();
    }

    // Sends a friend request to the target user — does NOT add them as friends yet.
    public void addFriend(User friend) {
        friend.addPendingRequest(user);
    }

    // Accepts an incoming request from requester. Both users are added to each other's friend list.
    public void acceptRequest(User requester) {
        if (!user.getPendingRequests().contains(requester)) {
            return;
        }
        user.removePendingRequest(requester);
        friends.add(requester);
        requester.getFriendList().add(user);
    }

    // Rejects an incoming request — removes from pending without adding as friend.
    public void rejectRequest(User requester) {
        user.removePendingRequest(requester);
    }

    public void removeFriend(User friend) {
        friends.remove(friend);
    }
}