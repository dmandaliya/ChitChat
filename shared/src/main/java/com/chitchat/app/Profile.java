package com.chitchat.app;

public class Profile {

    private String bio;
    private String status;
    private String profilePic; // later
    private String coverPic;   // later

    public Profile() {
        this.bio = "";
        this.status = "online";
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}