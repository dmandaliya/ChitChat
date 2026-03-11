package com.chitchat.app;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        User a = new User("Ayden", "Sendrea", "aydsman", "123");
        User b = new User("Jon", "Jones", "jonny", "123");
        List<User> users = new ArrayList<>();
        users.add(a);
        users.add(b);

        Conversation convo = MessageHandler.createConversation(users);
        System.out.println("Convo ID: " +  convo.getId() + "Chat members: " + convo.getMembers());
    }
}
