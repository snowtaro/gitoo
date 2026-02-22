package com.example.gitoo.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameStateResponse {
    private String roomId;
    private boolean started;
    private List<String> turnOrder;
    private String currentTurn;
    private String lastWord;         // 있으면 좋음
    private Integer turnIndex;       // 있으면 좋음
    private Long turnStartedAtEpoch; // 있으면 좋음(프론트에서 남은시간 계산)
}
