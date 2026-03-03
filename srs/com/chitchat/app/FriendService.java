package com.chitchat.app;

public class FriendService {

    public static void addFriend(User user, User friend) {
        if (!friend.getNewAccount()) { // if friend is not a new account (uninitialized) continue
            user.addToList(friend);
            System.out.println(user.getUsername() + " has added " + friend.getUsername());
        }
    }

    public void removeFriend(User user, User friend) {
        user.removeFromList(friend);
        System.out.println(user.getFname() + " has removed " + friend.getFname());
    }
}