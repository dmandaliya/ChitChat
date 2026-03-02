package com.chitchat.app;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserManager {
    private static final List<User> allUsers = new CopyOnWriteArrayList<>();

    public static void addUser(User user) {
        allUsers.add(user);
    }

    public static void removeUser(User user) {
        allUsers.remove(user);
    }

    public static List<User> getAllUsers() {
        return allUsers;
    }
}