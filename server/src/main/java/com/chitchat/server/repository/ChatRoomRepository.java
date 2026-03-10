package com.chitchat.server.repository;

import com.chitchat.server.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r JOIN r.members m WHERE m.username = :username")
    List<ChatRoom> findRoomsByUsername(@Param("username") String username);
}
