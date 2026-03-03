package com.chitchat.app;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserManager {
    private static final List<User> allUsers = new CopyOnWriteArrayList<>();
    private static final List<User> allOnlineUsers = new CopyOnWriteArrayList<>();

    public static void addUser(User user) {
        allUsers.add(user);
    }

    public static void removeUser(User user) {
        allUsers.remove(user);
    }

    public static void addOnlineUser(User user) {
        allOnlineUsers.add(user);
    }

    public static void removeOnlineUser(User user) {
        allOnlineUsers.remove(user);
    }

    public static boolean accessUser(User user) {
        for (User count : allUsers) {
            if (count.getUsername().equals(user.getUsername())) {   // check if this element is what you want
                return true;
            }
        }
        return false;
    }

    public static List<User> getAllUsers() {
        return allUsers;
    }

    public static List<User> getAllOnlineUsers() {
        return allOnlineUsers;
    }
}