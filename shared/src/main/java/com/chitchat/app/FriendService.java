package com.chitchat.app;

import java.util.List;

public class FriendService {
    private User user; /* com.chitchat.app.User is being imported into this class
                  so we can add the friend into its ArrayList. */
    private List<User> friends;

    public FriendService(User user) {
        this.user = user;
        this.friends = user.getFriendList();
    }

    // Sends a friend request to the target user — does NOT add them as friends yet.
    public void addFriend(User friend) {
        friend.addPendingRequest(user);
        System.out.println(user.getFname() + " sent a friend request to " + friend.getFname());
    }

    // Accepts an incoming request from requester. Both users are added to each other's friend list.
    public void acceptRequest(User requester) {
        if (!user.getPendingRequests().contains(requester)) {
            System.out.println("No pending request from " + requester.getFname());
            return;
        }
        user.removePendingRequest(requester);
        friends.add(requester);
        requester.getFriendList().add(user);
        System.out.println(user.getFname() + " and " + requester.getFname() + " are now friends!");
    }

    public void removeFriend(User friend) {
        friends.remove(friend);
        System.out.println(user.getFname() + " has removed " + friend.getFname());
    }
}