package com.chitchat.server.service;

import com.chitchat.server.model.ChatMessage;
import com.chitchat.server.repository.ChatMessageRepository;
import com.chitchat.shared.Message;
import com.chitchat.shared.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles saving and retrieving chat messages from the database.
 *
 * This service bridges the gap between the shared Message DTO (used for
 * WebSocket transport) and the ChatMessage entity (stored in the DB).
 * They're kept separate so we're free to evolve the DB schema without
 * breaking the wire format, and vice versa.
 */
@Service
public class ChatMessageService {

    private final ChatMessageRepository repo;

    public ChatMessageService(ChatMessageRepository repo) {
        this.repo = repo;
    }

    /**
     * Converts a Message DTO to a ChatMessage entity and persists it.
     *
     * We skip messages without an ID — those are ephemeral events like typing
     * indicators or read receipts that don't belong in the message history.
     * The ID is assigned by ChatController before calling this method.
     *
     * If the message arrives without a timestamp (shouldn't normally happen),
     * we default to the current server time rather than letting it stay null.
     */
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

    // Returns the last 50 public messages in chronological order.
    // We cap at 50 to keep the initial load fast — older history isn't shown.
    public List<ChatMessage> getPublicHistory() {
        return repo.findTop50ByTypeOrderByTimestampAsc(MessageType.PUBLIC_MESSAGE);
    }

    // Private history is bidirectional — we fetch messages where either user
    // was the sender or receiver. See the custom query in ChatMessageRepository.
    public List<ChatMessage> getPrivateHistory(String user1, String user2) {
        return repo.findPrivateMessages(user1, user2);
    }

    public List<ChatMessage> getRoomHistory(String roomId) {
        return repo.findTop50ByRoomIdOrderByTimestampAsc(roomId);
    }

    public void deleteMessage(String id, String requestingUser) {
        ChatMessage msg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!msg.getSender().equals(requestingUser)) {
            throw new IllegalArgumentException("You can only delete your own messages");
        }
        repo.deleteById(id);
    }
}
