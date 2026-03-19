package com.chitchat.server.model;

import com.chitchat.shared.UserPreferences;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fname;

    @Column(nullable = false)
    private String lname;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String hashedPassword;

    private String lastOnline;

    private boolean loggedIn = false;

    @Convert(converter = UserPreferencesConverter.class)
    @Column(columnDefinition = "TEXT")
    private UserPreferences preferences = new UserPreferences();

    @ManyToMany
    @JoinTable(
        name = "user_friends",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private List<UserEntity> friendList = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "user_blocked",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_id")
    )
    private List<UserEntity> blockedList = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "user_pending_requests",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "requester_id")
    )
    private List<UserEntity> pendingRequests = new ArrayList<>();

    public UserEntity() {}

    public UserEntity(String fname, String lname, String username, String hashedPassword) {
        this.fname = fname;
        this.lname = lname;
        this.username = username;
        this.hashedPassword = hashedPassword;
    }

    public Long getId() { return id; }

    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    public String getLastOnline() { return lastOnline; }
    public void setLastOnline(String lastOnline) { this.lastOnline = lastOnline; }

    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    public UserPreferences getPreferences() { return preferences; }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }

    public List<UserEntity> getFriendList() { return friendList; }

    public List<UserEntity> getBlockedList() { return blockedList; }

    public List<UserEntity> getPendingRequests() { return pendingRequests; }
}
