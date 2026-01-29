package com.example.gitoo.repository;

import com.example.gitoo.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomIdOrderByJoinedAtAsc(String roomId);
    boolean existsByRoomIdAndUserId(String roomId, Long userId);
    Optional<RoomMember> findByRoomIdAndUserId(String roomId, Long userId);
    void deleteByRoomIdAndUserId(String roomId, Long userId);
    long countByRoomId(String roomId);
}
