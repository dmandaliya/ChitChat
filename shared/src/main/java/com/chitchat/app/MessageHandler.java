package com.chitchat.app;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler {

    private static final List<Conversation> conversations = new ArrayList<>();
    private static int nextId = 1;

    // Returns true if a conversation with exactly the same set of members already exists
    public static boolean conversationExists(List<User> members) {
        for (Conversation c : conversations) {
            List<User> existing = c.getMembers();
            if (existing.size() == members.size() && existing.containsAll(members)) {
                return true;
            }
        }
        return false;
    }

    // Creates a new conversation with the given members and returns it
    public static Conversation createConversation(List<User> members) {
        if (conversationExists(members)) {
            System.out.println("Chat has already been created.");
            return null;
        }
        Conversation convo = new Conversation(nextId++, members);
        conversations.add(convo);
        for (User u : members) {
            u.addConversationId(convo.getId());
        }
        return convo;
    }

    // Finds a conversation by ID, returns null if not found
    public static Conversation getConversation(int id) {
        for (Conversation c : conversations) {
            if (c.getId() == id) return c;
        }
        return null;
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

    // Sends a message to a conversation. Does nothing if user is not a member.
    public static Message sendMessage(User user, int id, String content) {
        Conversation convo = getConversation(id);
        if (convo == null) return null;
        if (!convo.hasMember(user.getUsername())) {
            System.out.println("[ERROR: You are not a member of this conversation]");
            return null;
        }

        Message msg = new Message(user.getUsername(), content);
        convo.addMessage(msg);
        return msg;
    }

    // Prints all messages in a conversation in order
    public static void printConversation(Conversation convo) {
        System.out.println("\nConversation ID " + convo.getId());
        for (Message msg : convo.getMessages()) {
            System.out.println(msg.getSender() + ": " + msg.getContent());
        }
    }

    public static List<Conversation> getAllConversations() {
        return conversations;
    }
}
