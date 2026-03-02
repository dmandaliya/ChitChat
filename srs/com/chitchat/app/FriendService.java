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

    public void addFriend(User friend) {
        friends.add(friend);
        System.out.println(user.getFname() + " has added " + friend.getFname());
    }

    public void removeFriend(User friend) {
        friends.remove(friend);
        System.out.println(user.getFname() + " has removed " + friend.getFname());
    }
}