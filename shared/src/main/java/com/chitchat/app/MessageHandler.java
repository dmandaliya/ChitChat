package com.chitchat.app;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {

    private static final List<Conversation> conversations = new ArrayList<>();
    private static int nextId = 1;

    // Creates a new conversation with the given members and returns it
    public static Conversation createConversation(List<User> members) {
        Conversation convo = new Conversation(nextId++, members);
        conversations.add(convo);
        return convo;
    }

    // Finds a conversation by ID, returns null if not found
    public static Conversation getConversation(int id) {
        for (Conversation c : conversations) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    // Stores a message in the conversation and returns it.
    // Returns null if the conversation doesn't exist or the sender isn't a member.
    // Actual delivery to clients is handled by ClientHandler.
    public static Message storeMessage(int conversationId, User sender, String content) {
        Conversation convo = getConversation(conversationId);
        if (convo == null) return null;
        if (!convo.hasMember(sender.getUsername())) return null;

        Message msg = new Message(sender.getUsername(), content);
        convo.addMessage(msg);
        return msg;
    }

    // Returns the usernames of all members in a conversation (used by ClientHandler for delivery)
    public static List<String> getMembers(int conversationId) {
        Conversation convo = getConversation(conversationId);
        if (convo == null) return new ArrayList<>();
        List<String> usernames = new ArrayList<>();
        for (User u : convo.getMembers()) {
            usernames.add(u.getUsername());
        }
        return usernames;
    }

    public static List<Conversation> getAllConversations() {
        return conversations;
    }
}
