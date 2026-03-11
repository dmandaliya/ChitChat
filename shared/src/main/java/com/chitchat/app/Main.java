package com.chitchat.app;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // intitializing test users / convo
        User a = new User("Ayden", "Sendrea", "aydsman", "123");
        User b = new User("Jon", "Jones", "jonnyjon7", "123");
        User c = new User("Bob", "Bones", "bobby_the_chatter22", "123");
        User d = new User("Ronald", "Rones", "ron123", "123");
        List<User> users1 = new ArrayList<>();
        List<User> users2 = new ArrayList<>();
        List<User> users3 = new ArrayList<>();
        users1.add(a);
        users1.add(b);
        users2.add(c);
        users2.add(d);
        users3.add(a);
        users3.add(c);
        Conversation convo1 = MessageHandler.createConversation(users1);
        Conversation convo2 = MessageHandler.createConversation(users2);
        Conversation convo3 = MessageHandler.createConversation(users3);

        // printing out the convo ID and members
        System.out.println("Convo ID: " + convo1.getId() + " || Chat members: " + MessageHandler.getMembers(convo1.getId()));
        System.out.println("Convo ID: " + convo2.getId() + " || Chat members: " + MessageHandler.getMembers(convo2.getId()));
        System.out.println("Convo ID: " + convo3.getId() + " || Chat members: " + MessageHandler.getMembers(convo3.getId()));

        MessageHandler.sendMessage(a, convo1.getId(), "Hello Jon!");
        MessageHandler.sendMessage(b, convo1.getId(), "hi ayden wsp");
        MessageHandler.sendMessage(a, convo1.getId(), "Not much wbu");
        MessageHandler.sendMessage(b, convo1.getId(), "just chillin");

        MessageHandler.sendMessage(c, convo2.getId(), "hi bob");
        MessageHandler.sendMessage(d, convo2.getId(), "hi ronald");

        MessageHandler.sendMessage(a, convo3.getId(), "hi bob");
        MessageHandler.sendMessage(c, convo3.getId(), "hi ronald");

        MessageHandler.printConversation(convo1);
        MessageHandler.printConversation(convo2);
        MessageHandler.printConversation(convo3);

    }
}
