package com.example.gitoo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameStartMessage {
    private String type;   // "GAME_STARTED"
    private String roomId;

    public static GameStartMessage of(String roomId) {
        return new GameStartMessage("GAME_STARTED", roomId);
    }
}