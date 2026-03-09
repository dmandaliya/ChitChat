package com.chitchat.app;

public class Main {
    public static void main(String[] args) {
        User a = new User("Ayden", "Sendrea");
        User b = new User("Jon", "Smith");
        User c = new User("Bob", "Jones");

        FriendService friend_a = new FriendService(a);
        FriendService friend_b = new FriendService(b);
        FriendService friend_c = new FriendService(c);

        // A sends B a request — not friends yet
        friend_a.addFriend(b);

        // B accepts — both become friends
        friend_b.acceptRequest(a);

        // A tries to add C, but C never accepts — no friendship formed
        friend_a.addFriend(c);

        a.printList(a);
        b.printList(b);
    }
}