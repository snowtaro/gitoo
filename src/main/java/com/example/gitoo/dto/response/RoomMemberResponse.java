package com.example.gitoo.dto.response;
import com.example.gitoo.model.RoomMember;
import lombok.Builder;

@Builder
public record RoomMemberResponse(
        Long userId,
        String username,
        String role,
        boolean ready
) {}
