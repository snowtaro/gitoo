package com.example.gitoo.repository;

import com.example.gitoo.model.RoomMember;
import com.example.gitoo.model.RoomMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomIdOrderByJoinedAtAsc(String roomId);
    boolean existsByRoomIdAndUserId(String roomId, long userId);
    long countByRoomId(String roomId);
    void deleteByRoomIdAndUserId(String roomId, Long userId);
}
