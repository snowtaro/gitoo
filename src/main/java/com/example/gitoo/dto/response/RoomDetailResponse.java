package com.example.gitoo.dto.response;
import com.example.gitoo.model.Room;
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
