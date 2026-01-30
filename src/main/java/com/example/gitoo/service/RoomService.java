package com.example.gitoo.service;

import com.example.gitoo.dto.request.CreateRoomRequest;
import com.example.gitoo.dto.request.JoinRoomRequest;
import com.example.gitoo.dto.response.RoomDetailResponse;
import com.example.gitoo.dto.response.RoomMemberResponse;
import com.example.gitoo.dto.response.RoomResponse;
import com.example.gitoo.model.Room;
import com.example.gitoo.model.RoomMember;
import com.example.gitoo.model.RoomMemberRole;
import com.example.gitoo.model.User;
import com.example.gitoo.repository.RoomMemberRepository;
import com.example.gitoo.repository.RoomRepository;
import com.example.gitoo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ======================
    // JOIN
    // ======================
    @Transactional
    public RoomDetailResponse join(String roomId, JoinRoomRequest req, String username) {

        // 1) 유저 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "유저를 찾을 수 없습니다."
                ));

        // 2) 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "방이 없습니다."
                ));

        // 3) 비공개방 비번 검증
        if (room.isLocked()) {
            String raw = (req == null ? null : req.password());
            if (raw == null || raw.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 필요합니다.");
            }
            if (room.getPasswordHash() == null || !encoder.matches(raw, room.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 틀렸습니다.");
            }
        }

        // 4) 이미 참가중이면 그대로 반환 (idempotent)
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            RoomDetailResponse detail = detail(roomId);
            broadcastAfterChange(roomId, detail);
            return detail;
        }

        // 5) 정원 체크 (지금은 count로 체크)
        int now = (int) roomMemberRepository.countByRoomId(roomId);
        if (now >= room.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "정원이 가득 찼습니다.");
        }

        // 6) 멤버 저장 (RoomMember는 roomId/userId 기반!)
        boolean isHost = (now == 0);

        RoomMember member = RoomMember.builder()
                .roomId(roomId)
                .userId(user.getId())
                .username(user.getUsername())
                .role(isHost ? RoomMemberRole.HOST : RoomMemberRole.MEMBER)
                .ready(false)
                .build();

        roomMemberRepository.save(member);

        // 7) Room.nowPlayers를 쓰고 있다면 여기서 동기화 (선택이지만 너 코드가 nowPlayers를 갖고 있어서 맞춰줌)
        room.setNowPlayers(now + 1);
        roomRepository.save(room);

        // 8) 최신 상세 만들어서 broadcast + 반환
        RoomDetailResponse detail = detail(roomId);
        broadcastAfterChange(roomId, detail);
        return detail;
    }

    // ======================
    // LEAVE
    // ======================
    @Transactional
    public RoomDetailResponse leave(String roomId, String username) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방이 없습니다."));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유저를 찾을 수 없습니다."));

        boolean existed = roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId());
        if (existed) {
            roomMemberRepository.deleteByRoomIdAndUserId(roomId, user.getId());

            int now = Math.max(0, room.getNowPlayers() - 1);
            room.setNowPlayers(now);
            roomRepository.save(room);
        }

        // 방 비었으면 방 삭제(원하면 유지해도 됨)
        if (room.getNowPlayers() == 0) {
            roomRepository.delete(room);
            broadcastRooms();
            // 방이 삭제된 경우, 빈 상태 응답
            return RoomDetailResponse.builder()
                    .id(roomId)
                    .title(room.getTitle())
                    .maxPlayers(room.getMaxPlayers())
                    .nowPlayers(0)
                    .locked(room.isLocked())
                    .started(room.isStarted())
                    .members(List.of())
                    .build();
        }

        // 방장 나갔으면 위임 (첫 멤버를 HOST로)
        List<RoomMember> members = roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId);
        boolean hostExists = members.stream().anyMatch(m -> m.getRole() == RoomMemberRole.HOST);
        if (!hostExists && !members.isEmpty()) {
            RoomMember newHost = members.get(0);
            newHost.makeHost(); // role = HOST
            // JPA 영속이면 save 없어도 되지만, 확실하게 저장
            roomMemberRepository.save(newHost);
        }

        RoomDetailResponse detail = detail(roomId);
        broadcastAfterChange(roomId, detail);
        return detail;
    }

    // ======================
    // DETAIL
    // ======================
    @Transactional
    public RoomDetailResponse detail(String roomId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방이 없습니다."));

        List<RoomMember> members = roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId);

        // started는 room에 있다고 가정 (없으면 false로 고정)
        boolean started = room.isStarted();

        return RoomDetailResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .maxPlayers(room.getMaxPlayers())
                .nowPlayers(members.size()) // ✅ DB 기준이 더 안전함
                .locked(room.isLocked())
                .started(started)
                .members(members.stream()
                        .map(m -> RoomMemberResponse.builder()
                                .userId(m.getUserId())
                                .username(m.getUsername())
                                .role(m.getRole().name())
                                .ready(m.isReady())
                                .build()
                        )
                        .toList()
                )
                .build();
    }

    // ======================
    // CREATE
    // ======================
    @Transactional
    public RoomResponse create(CreateRoomRequest req) {
        boolean locked = req.usePassword();
        String hash = null;

        if (locked) {
            if (req.password() == null || req.password().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호를 입력하세요.");
            }
            hash = encoder.encode(req.password());
        }

        Room room = Room.builder()
                .title(req.title())
                .maxPlayers(req.maxPlayers())
                .nowPlayers(0)
                .locked(locked)
                .passwordHash(hash)
                .started(false)
                .build();

        Room saved = roomRepository.save(room);
        broadcastRooms();
        return RoomResponse.from(saved);
    }

    // ======================
    // LIST
    // ======================
    @Transactional
    public List<RoomResponse> list() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    // ======================
    // BROADCAST
    // ======================
    public void broadcastRooms() {
        List<RoomResponse> rooms = list();
        messagingTemplate.convertAndSend("/topic/rooms", rooms);
    }

    public void broadcastRoom(String roomId, RoomDetailResponse detail) {
        System.out.println("[WS] /topic/rooms/" + roomId + " send, members=" + detail.members().size());
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, detail);
    }

    private void broadcastAfterChange(String roomId, RoomDetailResponse detail) {
        broadcastRooms();              // 로비 갱신
        broadcastRoom(roomId, detail); // 대기방 갱신
    }
}
