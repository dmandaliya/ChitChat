package com.chitchat.app;

import java.util.ArrayList;
import java.util.List;

public class User {

    // -------- Values --------
    private String Fname; // First name
    private String Lname; // Last name
    private String username;
    private String userPassword; // Password String that user chose
    private String hashedPassword; // Used for lig
    private List<User> friendList = new ArrayList<>(); // Friends list
    private List<User> pendingRequests = new ArrayList<>(); // Incoming friend requests
    private String lastOnline; // If online, it puts time in this variable. When offline it stops updating.
    private PreferenceService preference; // Preference OBJ for each user
    private boolean newAccount = true; // Default true until values are chosen.
    private boolean loggedIn = false;
    private List<Integer> conversationIds = new ArrayList<>();
    private List<Integer> pendingChatNotifications = new ArrayList<>();
    private Profile profile = new Profile();
    private List<User> blockedUsers = new ArrayList<>();

    public User() {
        // For initializing a user with all empty values.
    }

    // If you want to define a user with a full name instead of all empty values.
    public User (String fname, String lname) {
        this.Fname = fname;
        this.Lname = lname;
        this.newAccount = false;
    }

    public User (String fname, String lname, String username, String password) {
        this.Fname = fname;
        this.Lname = lname;
        this.username = username;
        this.userPassword = password;
        this.hashedPassword = EncryptionService.hashPassword(password);
        this.newAccount = false;
    }

    // -------- Set/Get Fname (first name) --------
    public void setFname(String fname) {
        this.Fname = fname;
    }
    public String getFname() {
        return Fname;
    }

    // -------- Set/Get Lname (last name) --------
    public void setLname(String lname) {
        this.Lname = lname;
    }
    public String getLname() {
        return Lname;
    }

    // -------- Set/Get Lname (last name) --------
    public boolean getNewAccount() {return newAccount;}
    public void setNewAccount(boolean newAccount) {this.newAccount = newAccount;}

    // -------- Set/Get username --------
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }

    // -------- Set/Get password --------
    public void setPassword(String password) {
        this.userPassword = password;
        this.hashedPassword = EncryptionService.hashPassword(password);
    }
    public String getPassword() {
        return this.userPassword;
    }
    public String getHashedPassword() {
        return this.hashedPassword;
    }

    // -------- Get Friendlist --------
    public List<User> getFriendList() {
        return friendList;
    }

    // -------- Get/Add/Remove pending requests --------
    public List<User> getPendingRequests() {
        return pendingRequests;
    }
    public void addPendingRequest(User requester) {
        pendingRequests.add(requester);
    }
    public void removePendingRequest(User requester) {
        pendingRequests.remove(requester);
    }

    // -------- Set/Get loggedIn --------
    public boolean getLoggedIn() {return loggedIn;}
    public void setLoggedIn(boolean loggedIn) {this.loggedIn = loggedIn;}

    // -------- Set/Get password --------
    public void setLastOnline(String lastOnline) {
        this.lastOnline = lastOnline;
    }

    public String getLastOnline() {
        return lastOnline;
    }

    // -------- Set/Get preference OBJ --------
    public PreferenceService getPreference() {
        return preference;
    }
    public void setPreference(PreferenceService preference) {
        this.preference = preference;
    }

    // -------- Block list --------
    public List<User> getBlockedUsers() { return blockedUsers; }
    public void blockUser(User u) { blockedUsers.add(u); }
    public void unblockUser(User u) { blockedUsers.remove(u); }
    public boolean hasBlocked(User u) { return blockedUsers.contains(u); }

    // -------- Get profile --------
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    // -------- Get/Add conversationIds --------
    public List<Integer> getConversationIds() { return conversationIds; }
    public void addConversationId(int id) { conversationIds.add(id); }

    // -------- Pending chat notifications (shown once on next login) --------
    public List<Integer> getPendingChatNotifications() { return pendingChatNotifications; }
    public void addPendingChatNotification(int id) { pendingChatNotifications.add(id); }
    public void clearPendingChatNotifications() { pendingChatNotifications.clear(); }

    public void printList(User u) {
        System.out.print("Friends: ");
        for (User i: u.getFriendList()) {
            System.out.print(i.getFname() + " " + i.getLname() + ", ");
        }
    }
}
