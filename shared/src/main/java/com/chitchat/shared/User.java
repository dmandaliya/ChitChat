package com.chitchat.shared;

import java.util.ArrayList;
import java.util.List;

public class User {

    // -------- Values --------
    private String fname;
    private String lname;
    private String username;
    private String hashedPassword;
    private List<User> friendList = new ArrayList<>();
    private List<User> pendingRequests = new ArrayList<>(); // Incoming friend requests
    private String lastOnline;
    private UserPreferences preferences = new UserPreferences();
    private boolean newAccount = true; // Default true until values are chosen
    private boolean loggedIn = false;

    public User() {}

    public User(String fname, String lname) {
        this.fname = fname;
        this.lname = lname;
    }

    // -------- Set/Get fname --------
    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    // -------- Set/Get lname --------
    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }

    // -------- Set/Get newAccount --------
    public boolean getNewAccount() { return newAccount; }
    public void setNewAccount(boolean newAccount) { this.newAccount = newAccount; }

    // -------- Set/Get username --------
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // -------- Set/Get hashedPassword --------
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    // -------- Get/Add/Remove friendList --------
    public List<User> getFriendList() { return friendList; }
    public void addToList(User friend) { friendList.add(friend); }
    public void removeFromList(User friend) { friendList.remove(friend); }

    // -------- Get/Add/Remove pendingRequests --------
    public List<User> getPendingRequests() { return pendingRequests; }
    public void addPendingRequest(User requester) { pendingRequests.add(requester); }
    public void removePendingRequest(User requester) { pendingRequests.remove(requester); }

    // -------- Set/Get loggedIn --------
    public boolean getLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    // -------- Set/Get lastOnline --------
    public String getLastOnline() { return lastOnline; }
    public void setLastOnline(String lastOnline) { this.lastOnline = lastOnline; }

    // -------- Set/Get preferences --------
    public UserPreferences getPreferences() { return preferences; }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }

    public void printList(User u) {
        System.out.print("Friends: ");
        for (User i : u.getFriendList()) {
            System.out.print(i.getFname() + " " + i.getLname() + " | ");
        }
        System.out.println(" ");
    }
}