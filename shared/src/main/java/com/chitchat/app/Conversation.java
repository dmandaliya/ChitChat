package com.chitchat.app;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private int id;
    private List<User> members;
    private List<Message> messages;

    public Conversation(int id, List<User> members) {
        this.id = id;
        this.members = new ArrayList<>(members);
        this.messages = new ArrayList<>();
    }

    public int getId() { return id; }
    public List<User> getMembers() { return members; }
    public List<Message> getMessages() { return messages; }

    public void addMessage(Message message) {
        messages.add(message);
    }

    // Check if a user (by username) is a member of this conversation
    public boolean hasMember(String username) {
        return members.stream().anyMatch(u -> u.getUsername().equals(username));
    }
}
