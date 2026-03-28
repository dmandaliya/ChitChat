package com.chitchat.shared;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The main data object passed between the client and server over WebSocket and REST.
 *
 * We use a single Message class for everything — chat messages, system events,
 * typing indicators, read receipts, call signals — and rely on the 'type' field
 * to tell the receiver what to do with it. Keeping one unified DTO makes
 * the WebSocket handler much simpler since all STOMP frames carry the same shape.
 *
 * The no-arg constructor is required by Jackson for deserialization. Without it,
 * Spring's @Payload binding breaks silently (messages arrive as null).
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // UUID assigned by the server when a message is first persisted.
    // Clients use this ID to send read receipts back — the server looks it up
    // in the messageSenders map to know who to notify.
    private String id;

    private MessageType type;
    private String sender;
    private String receiver;  // null for public/room messages, set for DMs and call signals
    private String content;

    // Only set for group chat messages — maps to a ChatRoom's ID.
    // Null for public and private messages.
    private String roomId;

    private LocalDateTime timestamp;
    private boolean edited;
    private boolean deleted;

    // Required by Jackson for deserialization — do not remove.
    public Message() {}

    public Message(MessageType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
