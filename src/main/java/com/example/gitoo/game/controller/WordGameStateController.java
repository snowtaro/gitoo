package com.example.gitoo.game.controller;

import com.example.gitoo.game.dto.GameStateResponse;
import com.example.gitoo.game.service.WordGameStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class WordGameStateController {

    private final WordGameStateService wordGameStateService;

    @GetMapping("/{roomId}/game-state")
    public GameStateResponse getGameState(@PathVariable String roomId) {
        return wordGameStateService.getState(roomId);
    }
}
