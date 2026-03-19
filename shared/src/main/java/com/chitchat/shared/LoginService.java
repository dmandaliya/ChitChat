package com.chitchat.shared;

public class LoginService {

    // Sets up a new account and registers the user in UserManager.
    public static void signup(User user, String fname, String lname, String username, String hashedPassword) {
        user.setFname(fname);
        user.setLname(lname);
        user.setUsername(username);
        user.setHashedPassword(hashedPassword);
        user.setNewAccount(false);
        UserManager.addUser(user);
        user.setLoggedIn(true);
    }

    // Marks an existing user as logged in.
    public static void login(User user) {
        if (!user.getNewAccount()) {
            user.setLoggedIn(true);
        }
    }

    // Marks a user as logged out.
    public static void logout(User user) {
        user.setLoggedIn(false);
    }
}