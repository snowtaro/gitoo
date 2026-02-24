package com.example.gitoo.room;

import com.example.gitoo.game.dto.GameStartMessage;
import com.example.gitoo.game.service.WordGameStateService;
import com.example.gitoo.user.model.User;
import com.example.gitoo.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

        private final RoomRepository roomRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final RoomMemberRepository roomMemberRepository;
        private final UserRepository userRepository;
        private final WordGameStateService wordGameStateService;
        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // ======================
        // JOIN
        // ======================
        @Transactional
        public RoomDetailResponse join(String roomId, JoinRoomRequest req, String username) {

                // 1) 유저 조회
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "유저를 찾을 수 없습니다."));

                // 2) 방 조회
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "방이 없습니다."));

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
                                .nickname(user.getNickname())
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
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "유저를 찾을 수 없습니다."));

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
                                                                .nickname(m.getNickname())
                                                                .username(m.getUsername())
                                                                .role(m.getRole().name())
                                                                .ready(m.isReady())
                                                                .build())
                                                .toList())
                                .build();
        }

        // ======================
        // CREATE
        // ======================
        @Transactional
        public RoomResponse create(CreateRoomRequest req, String username) {
                boolean locked = req.usePassword();
                String hash = null;

                if (locked) {
                        if (req.password() == null || req.password().isBlank()) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호를 입력하세요.");
                        }
                        hash = encoder.encode(req.password());
                }

                Room room = Room.builder()
                                .id(java.util.UUID.randomUUID().toString())
                                .title(req.title())
                                .maxPlayers(req.maxPlayers())
                                .nowPlayers(0)
                                .locked(locked)
                                .passwordHash(hash)
                                .started(false)
                                .build();

                Room saved = roomRepository.saveAndFlush(room);

                JoinRoomRequest joinReq = req.usePassword() ? new JoinRoomRequest(req.password()) : null;
                join(saved.getId(), joinReq, username);

                Room updated = roomRepository.findById(saved.getId()).orElse(saved);
                return RoomResponse.from(updated);
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
                broadcastRooms(); // 로비 갱신
                broadcastRoom(roomId, detail); // 대기방 갱신
        }

        @Transactional
        public RoomDetailResponse toggleReady(String roomId, String username) {

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "유저를 찾을 수 없습니다."));

                RoomMember me = roomMemberRepository
                                .findByRoomIdAndUserId(roomId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "방에 참가하지 않은 유저입니다."));

                // 방장은 ready 불가
                if (me.getRole() == RoomMemberRole.HOST) {
                        return detail(roomId);
                }

                me.setReady(!me.isReady());

                RoomDetailResponse room = detail(roomId);
                broadcastAfterChange(roomId, room);

                return room;
        }

        @Transactional
        public void start(String roomId, String username) {

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "유저를 찾을 수 없습니다."));

                RoomMember host = roomMemberRepository
                                .findByRoomIdAndUserId(roomId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "방에 참가하지 않았습니다."));

                // 1️⃣ 방장만 시작 가능
                if (host.getRole() != RoomMemberRole.HOST) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN, "방장만 시작할 수 있습니다.");
                }

                List<RoomMember> members = roomMemberRepository.findByRoomId(roomId);

                // 2️⃣ 방장 제외 전원 ready 체크
                boolean allReady = members.stream()
                                .filter(m -> m.getRole() != RoomMemberRole.HOST)
                                .allMatch(RoomMember::isReady);

                if (!allReady) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "모든 사람이 ready가 되야합니다");
                }

                // 3️⃣ 게임 시작 처리
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "방이 없습니다."));

                room.setStarted(true);
                roomRepository.save(room);
                // 4️⃣ 턴 순서 생성 (랜덤 섞기) ✅
                List<String> turnOrder = members.stream()
                                .map(RoomMember::getNickname)
                                .collect(Collectors.toList());

                Collections.shuffle(turnOrder); //
                // (선택) ready 초기화

                members.forEach(m -> m.setReady(false));
                roomMemberRepository.saveAll(members);

                String currentTurn = turnOrder.isEmpty() ? null : turnOrder.getFirst();
                wordGameStateService.saveStartedState(roomId, turnOrder, currentTurn);

                GameStartMessage msg = new GameStartMessage("GAME_STARTED", roomId, turnOrder, currentTurn);

                messagingTemplate.convertAndSend("/topic/rooms/" + roomId, msg);
                messagingTemplate.convertAndSend("/topic/game/" + roomId, msg);
        }

}
