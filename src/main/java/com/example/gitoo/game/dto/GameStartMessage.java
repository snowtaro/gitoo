package com.example.gitoo.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameStartMessage {
    private String type;   // "GAME_STARTED"
    private String roomId;
    List<String> turnOrder;
    String currentTurn;
    public static GameStartMessage of(String roomId, List<String> turnOrder) {
        return new GameStartMessage("GAME_STARTED", roomId, turnOrder, turnOrder.isEmpty() ? null : turnOrder.getFirst());
    }
}