package com.chitchat.server.repository;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.shared.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    // Last 50 public messages
    List<ChatMessage> findTop50ByTypeOrderByTimestampAsc(MessageType type);

    // Private messages between two users (both directions)
    @Query("SELECT m FROM ChatMessage m WHERE m.type = 'PRIVATE_MESSAGE' " +
           "AND ((m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findPrivateMessages(@Param("u1") String u1, @Param("u2") String u2);

    // Last 50 room messages
    List<ChatMessage> findTop50ByRoomIdOrderByTimestampAsc(String roomId);
}
