package com.example.gitoo.service;
import com.example.gitoo.dto.request.CreateRoomRequest;
import com.example.gitoo.dto.request.JoinRoomRequest;
import com.example.gitoo.dto.response.RoomResponse;
import com.example.gitoo.model.Room;
import com.example.gitoo.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 이미 Security에 PasswordEncoder Bean 있으면 그걸 주입해도 됨
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public RoomResponse create(CreateRoomRequest req) {
        boolean locked = req.usePassword();
        String hash = null;

        if (locked) {
            if (req.password() == null || req.password().isBlank()) {
                throw new IllegalArgumentException("비밀번호를 입력하세요.");
            }
            hash = encoder.encode(req.password());
        }

        Room room = Room.builder()
                .title(req.title())
                .maxPlayers(req.maxPlayers())
                .nowPlayers(0)
                .locked(locked)
                .passwordHash(hash)
                .build();

        Room saved = roomRepository.save(room);
        broadcastRooms();
        return RoomResponse.from(saved);
    }

    @Transactional
    public void join(String roomId, JoinRoomRequest req) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (room.isLocked()) {
            String pw = (req == null ? null : req.password());
            if (pw == null || pw.isBlank()) throw new IllegalArgumentException("비밀번호가 필요합니다.");
            if (room.getPasswordHash() == null || !encoder.matches(pw, room.getPasswordHash())) {
                throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
            }
        }

        room.join(); // nowPlayers++
        // dirty checking으로 update
        broadcastRooms();
    }

    @Transactional
    public List<RoomResponse> list() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    public void broadcastRooms() {
        List<RoomResponse> rooms = roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
        messagingTemplate.convertAndSend("/topic/rooms", rooms);
    }
}
