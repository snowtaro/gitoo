package com.example.gitoo.room;
import lombok.Builder;

import java.util.List;

@Builder
public record RoomDetailResponse(
        String id,
        String title,
        int maxPlayers,
        int nowPlayers,
        boolean locked,
        boolean started,
        List<RoomMemberResponse> members
) {}
