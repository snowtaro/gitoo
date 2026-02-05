package com.example.gitoo.controller;

import com.example.gitoo.dto.response.GameStateResponse;
import com.example.gitoo.service.WordGameStateService;
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
