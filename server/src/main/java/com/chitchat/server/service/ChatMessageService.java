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

    public ChatMessage editMessage(String id, String newContent, String requestingUser) {
        ChatMessage msg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + id));
        if (!msg.getSender().equals(requestingUser)) {
            throw new IllegalArgumentException("Cannot edit another user's message");
        }
        msg.setContent(newContent);
        msg.setEdited(true);
        return repo.save(msg);
    }

    public ChatMessage deleteMessage(String id, String requestingUser) {
        ChatMessage msg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + id));
        if (!msg.getSender().equals(requestingUser)) {
            throw new IllegalArgumentException("Cannot delete another user's message");
        }
        msg.setContent("Message deleted");
        msg.setDeleted(true);
        return repo.save(msg);
    }

    public List<ChatMessage> searchPrivateMessages(String user1, String user2, String keyword) {
        return repo.searchPrivateMessages(user1, user2, keyword);
    }
}
