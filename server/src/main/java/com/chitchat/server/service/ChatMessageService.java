package com.chitchat.server.service;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.server.repository.ChatMessageRepository;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repo;

    public ChatMessageService(ChatMessageRepository repo) {
        this.repo = repo;
    }

    public void save(Message msg) {
        if (msg.getId() == null) return;
        ChatMessage cm = new ChatMessage();
        cm.setId(msg.getId());
        cm.setType(msg.getType());
        cm.setSender(msg.getSender());
        cm.setReceiver(msg.getReceiver());
        cm.setRoomId(msg.getRoomId());
        cm.setContent(msg.getContent());
        cm.setTimestamp(msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now());
        repo.save(cm);
    }

    public List<ChatMessage> getPublicHistory() {
        return repo.findTop50ByTypeOrderByTimestampAsc(MessageType.PUBLIC_MESSAGE);
    }

    public List<ChatMessage> getPrivateHistory(String user1, String user2) {
        return repo.findPrivateMessages(user1, user2);
    }

    public List<ChatMessage> getRoomHistory(String roomId) {
        return repo.findTop50ByRoomIdOrderByTimestampAsc(roomId);
    }
}
