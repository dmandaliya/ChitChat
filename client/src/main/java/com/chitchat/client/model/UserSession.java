package com.chitchat.client.model;

public class UserSession {

    private static UserSession instance;

    private String username;
    private String fname;
    private String lname;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }

    public String getDisplayName() { return fname + " " + lname; }

    public void clear() {
        username = null;
        fname = null;
        lname = null;
    }
}
