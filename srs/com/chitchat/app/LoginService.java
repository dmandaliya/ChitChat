package com.chitchat.app;

import java.util.Scanner;
import java.util.List;

public class LoginService {
    private static List<User> allUsers;
    private static User empty = new User(); // To check if anyone is logged out.
    private static User currentUser = empty;

    // Mainly for debugging
    public static void display(User user){
        System.out.println("Name: " + user.getFname() + " " + user.getLname());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Password: " + user.getPassword());
        System.out.println("Hashed Password: " + user.getHashedPassword());
        user.printList(user);
    }

    // This format is for terminal, will change to use for GUI later.
    public static void initialize(User user, String Fname, String Lname, String username, String password) {
        user.setFname(Fname);
        user.setLname(Lname);
        user.setUsername(username);
        user.setPassword(password);
        user.setNewAccount(false);
        UserManager.addUser(user);
        user.setLoggedIn(true);
        display(user);
    }

    public static void login(User user, String Fname, String Lname, String username, String password) {
        System.out.println(user.getNewAccount());
        if ((currentUser == empty) && (!user.getNewAccount())) {
            display(user);
            user.setLoggedIn(true);
        }
        else if ((currentUser == empty) && (user.getNewAccount())) initialize(user, Fname, Lname, username, password);
        else {
            System.out.println("com.chitchat.app.User is currently logged in.");
            return;
        }
        System.out.println("Logged in: " + user.getUsername() + "\n");
        currentUser = user;
    }

    public static void logout(User user) {
        currentUser = empty;
        user.setLoggedIn(false);
        System.out.println("Logged out: " + user.getUsername() + "\n");
    }
}
