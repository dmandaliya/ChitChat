package com.chitchat.server.service;

import com.chitchat.server.model.ChatRoom;
import com.chitchat.server.model.UserEntity;
import com.chitchat.server.repository.ChatRoomRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository roomRepository;
    private final UserService userService;

    public ChatRoomService(ChatRoomRepository roomRepository, UserService userService) {
        this.roomRepository = roomRepository;
        this.userService = userService;
    }

    public ChatRoom createRoom(String name, String description, String createdByUsername) {
        UserEntity creator = userService.findByUsername(createdByUsername);
        ChatRoom room = new ChatRoom(name, description, createdByUsername);
        room.getMembers().add(creator);
        return roomRepository.save(room);
    }

    public ChatRoom joinRoom(@NonNull Long roomId, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        UserEntity user = userService.findByUsername(username);
        if (!room.getMembers().contains(user)) {
            room.getMembers().add(user);
            roomRepository.save(room);
        }
        return room;
    }

    public void leaveRoom(@NonNull Long roomId, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        UserEntity user = userService.findByUsername(username);
        room.getMembers().remove(user);
        roomRepository.save(room);
    }

    public List<ChatRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<ChatRoom> getRoomsForUser(String username) {
        return roomRepository.findRoomsByUsername(username);
    }

    public ChatRoom getRoom(@NonNull Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }
}
