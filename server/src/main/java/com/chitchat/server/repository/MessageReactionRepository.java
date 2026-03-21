package com.chitchat.server.repository;

import com.chitchat.server.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
}
