package com.example.gitoo.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordGameStateService {

    private final WordGameStateRepository wordGameStateRepository;
    private final ObjectMapper objectMapper;

    public GameStateResponse getState(String roomId) {
        WordGameState state = wordGameStateRepository.findById(roomId).orElse(null);

        if (state == null) {
            return new GameStateResponse(roomId, false, List.of(), null, null, null, null);
        }

        List<String> order = readTurnOrder(state.getTurnOrderJson());

        Long turnStartedAtEpoch = state.getTurnStartedAt() == null
                ? null
                : state.getTurnStartedAt().toEpochMilli();

        return new GameStateResponse(
                roomId,
                state.isStarted(),
                order,
                state.getCurrentTurn(),
                state.getLastWord(),
                state.getTurnIndex(),
                turnStartedAtEpoch);
    }

    public void saveStartedState(String roomId, List<String> turnOrder, String currentTurn) {
        String json = writeTurnOrder(turnOrder);
        String eliminatedJson = writeTurnOrder(List.of()); // 빈 리스트
        String rankingsJson = writeTurnOrder(List.of());

        WordGameState state = WordGameState.builder()
                .roomId(roomId)
                .started(true)
                .turnOrderJson(json)
                .eliminatedPlayersJson(eliminatedJson)
                .rankingsJson(rankingsJson)
                .turnIndex(0)
                .currentTurn(currentTurn)
                .lastWord(null)
                .turnStartedAt(Instant.now())
                .build();

        wordGameStateRepository.save(state);
    }

    private String writeTurnOrder(List<String> turnOrder) {
        try {
            return objectMapper.writeValueAsString(turnOrder);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "turnOrder 직렬화 실패", e);
        }
    }

    private List<String> readTurnOrder(String json) {
        try {
            if (json == null || json.isBlank())
                return List.of();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "turnOrder 역직렬화 실패", e);
        }
    }

    public WordGameState getRawState(String roomId) {
        return wordGameStateRepository.findById(roomId).orElse(null);
    }

    public void saveState(WordGameState state) {
        wordGameStateRepository.save(state);
    }
}
