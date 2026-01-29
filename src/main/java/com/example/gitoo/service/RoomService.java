package com.example.gitoo.service;
import com.example.gitoo.dto.request.CreateRoomRequest;
import com.example.gitoo.dto.request.JoinRoomRequest;
import com.example.gitoo.dto.response.RoomDetailResponse;
import com.example.gitoo.dto.response.RoomResponse;
import com.example.gitoo.model.Room;
import com.example.gitoo.model.RoomMember;
import com.example.gitoo.repository.RoomMemberRepository;
import com.example.gitoo.repository.RoomRepository;
import com.example.gitoo.repository.UserRepository;
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
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository; // username -> userId 얻기 위해
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
    public RoomDetailResponse detail(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        var members = roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId);

        return RoomDetailResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .max(room.getMaxPlayers())
                .now(room.getNowPlayers())
                .locked(room.isLocked())
                .members(members.stream().map(m ->
                        RoomDetailResponse.Member.builder()
                                .userId(m.getUserId())
                                .username(m.getUsername())
                                .role(m.getRole().name())
                                .ready(m.isReady())
                                .schoolName(null)
                                .build()
                ).toList())
                .build();
    }


    @Transactional
    public RoomDetailResponse join(String roomId, JoinRoomRequest req, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (room.isLocked()) {
            String pw = (req == null ? null : req.password());
            if (pw == null || pw.isBlank()) throw new IllegalArgumentException("비밀번호가 필요합니다.");
            if (room.getPasswordHash() == null || !encoder.matches(pw, room.getPasswordHash())) {
                throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
            }
        }

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 이미 참가중이면 nowPlayers++ 하면 안 됨
        boolean already = roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId());
        if (!already) {
            room.join(); // nowPlayers++

            boolean first = (room.getNowPlayers() == 1);
            roomMemberRepository.save(RoomMember.builder()
                    .roomId(roomId)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(first ? RoomMember.Role.HOST : RoomMember.Role.MEMBER)
                    .ready(false)
                    .build());
        }

        broadcastRooms();                 // 로비 목록 갱신
        RoomDetailResponse detail = detail(roomId);
        broadcastRoom(detail);            // ⭐ 대기방 갱신
        return detail;
    }

    @Transactional
    public RoomDetailResponse leave(String roomId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        boolean existed = roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId());
        if (existed) {
            roomMemberRepository.deleteByRoomIdAndUserId(roomId, user.getId());
            room.setNowPlayers(Math.max(0, room.getNowPlayers() - 1)); // nowPlayers 감소
        }

        // 방에 아무도 없으면 방 삭제(선택)
        if (room.getNowPlayers() == 0) {
            roomRepository.delete(room);
            broadcastRooms();
            return RoomDetailResponse.builder()
                    .id(roomId).title(room.getTitle()).max(room.getMaxPlayers()).now(0).locked(room.isLocked())
                    .members(List.of())
                    .build();
        }

        // HOST가 없으면 첫 멤버를 HOST로 (선택)
        var members = roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId);
        boolean hostExists = members.stream().anyMatch(m -> m.getRole() == RoomMember.Role.HOST);
        if (!hostExists && !members.isEmpty()) {
            RoomMember newHost = members.get(0);
            // 영속 객체면 setRole만 해도 됨
            // newHost.setRole(RoomMember.Role.HOST);
            roomMemberRepository.save(RoomMember.builder()
                    .id(newHost.getId())
                    .roomId(newHost.getRoomId())
                    .userId(newHost.getUserId())
                    .username(newHost.getUsername())
                    .role(RoomMember.Role.HOST)
                    .ready(newHost.isReady())
                    .joinedAt(newHost.getJoinedAt())
                    .build());
        }

        broadcastRooms();
        RoomDetailResponse detail = detail(roomId);
        broadcastRoom(detail);
        return detail;
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
    public void broadcastRoom(RoomDetailResponse detail) {
        messagingTemplate.convertAndSend("/topic/rooms/" + detail.getId(), detail);
    }
}
