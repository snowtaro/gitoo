package com.example.gitoo.room;
import lombok.Builder;

@Builder
public record RoomMemberResponse(
        Long userId,
        String username,
        String role,
        String nickname,
        boolean ready
) {}
