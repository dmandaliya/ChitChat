package com.chitchat.server.model;

import com.chitchat.shared.MessageType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    private String id; // UUID from ChatController

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private String sender;
    private String receiver; // null for public / room
    private String roomId;   // null for public / private

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<MessageReaction> reactions = new ArrayList<>();

    public ChatMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public List<MessageReaction> getReactions() { return reactions; }
}
