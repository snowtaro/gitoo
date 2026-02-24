package com.example.gitoo.game.service;

import com.example.gitoo.game.dto.GameStateResponse;
import com.example.gitoo.game.dto.WordChainMessage;
import com.example.gitoo.game.model.WordGameState;
import com.example.gitoo.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordChainService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WordGameStateService wordGameStateService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final DictionaryLoader dictionaryLoader;
    // 마지막으로 사용된 단어를 방별로 저장 (Deprecate or Sync with DB? For now keep it as cache)
    private final Map<String, String> lastWordByRoom = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> usedWordsByRoom = new ConcurrentHashMap<>();
    /**
     * 단어 검증 (기존 메서드 유지)
     */
    public String validateWord(String word, String roomId) {
        if (word == null || word.trim().isEmpty()) {
            return "단어를 입력해주세요.";
        }
        if (!word.matches("^[가-힣]+$")) {
            return "한글만 입력 가능합니다.";
        }
        if (!dictionaryLoader.contains(word)) {
            return "사전에 없는 단어입니다.";
        }

        Set<String> usedWords = usedWordsByRoom.getOrDefault(roomId, new HashSet<>());
        if (usedWords.contains(word)) {
            return "이미 사용된 단어입니다.";
        }
        // DB state check preferred if available, but let's use memory for now
        String lastWord = lastWordByRoom.get(roomId);
        if (lastWord != null) {
            char lastChar = lastWord.charAt(lastWord.length() - 1);
            char firstChar = word.charAt(0);
            if (lastChar != firstChar) {
                return "'" + lastChar + "'(으)로 시작하는 단어를 입력해주세요.";
            }
        }
        return "SUCCESS";
    }

    /**
     * 단어 제출 처리
     */
    public void handleWord(WordChainMessage message) {
        String roomId = message.getRoomId();
        String word = message.getWord();

        String validationResult = validateWord(word, roomId);

        if ("SUCCESS".equals(validationResult)) {
            // 1) 메모리 캐시 갱신
            usedWordsByRoom.computeIfAbsent(roomId, k -> new HashSet<>()).add(word);
            lastWordByRoom.put(roomId, word);

            // 2) DB 게임 상태 갱신 (턴 넘김 + 마지막 단어 + 타이머 시작시각)
            WordGameState state = wordGameStateService.getRawState(roomId);

            if (state != null && state.isStarted()) {
                List<String> order = parseList(state.getTurnOrderJson());

                // 탈락자 제외한 생존자 기준으로 턴 계산 (권장)
                List<String> eliminated = parseList(state.getEliminatedPlayersJson());
                List<String> aliveOrder = order.stream()
                        .filter(u -> !eliminated.contains(u))
                        .toList();

                if (!aliveOrder.isEmpty()) {
                    // 현재 턴이 aliveOrder에 없는 예외 상황 방어
                    int currentIdx = aliveOrder.indexOf(state.getCurrentTurn());
                    if (currentIdx < 0) currentIdx = 0;

                    int nextIdx = (currentIdx + 1) % aliveOrder.size();
                    String nextTurn = aliveOrder.get(nextIdx);

                    // turnIndex는 기존 전체 turnOrder 기준 인덱스로 저장 (프론트/기존 로직 호환)
                    int nextTurnIndexInFullOrder = order.indexOf(nextTurn);

                    state.setLastWord(word);
                    state.setTurnStartedAt(java.time.Instant.now());
                    state.setCurrentTurn(nextTurn);
                    state.setTurnIndex(nextTurnIndexInFullOrder >= 0 ? nextTurnIndexInFullOrder : 0);

                    wordGameStateService.saveState(state);
                }
            }

            // 3) 브로드캐스트 메시지 설정
            message.setType(WordChainMessage.MessageType.WORD);
            message.setMessage(message.getUsername() + ": " + word);

        } else {
            message.setType(WordChainMessage.MessageType.ERROR);
            message.setMessage(validationResult);
        }

        // 4) 전송
        broadcastToRoom(roomId, message);
    }



    public void handleJoin(WordChainMessage message) {
        message.setType(WordChainMessage.MessageType.JOIN);
        message.setMessage(message.getUsername() + "님이 게임에 참가했습니다.");
        broadcastToRoom(message.getRoomId(), message);
    }

    public void handleLeave(WordChainMessage message) {
        message.setType(WordChainMessage.MessageType.LEAVE);
        message.setMessage(message.getUsername() + "님이 게임을 떠났습니다.");
        broadcastToRoom(message.getRoomId(), message);
    }

    /**
     * 탈락 처리
     */
    public void handleElimination(WordChainMessage message) {
        String roomId = message.getRoomId();
        String username = message.getUsername();

        // [디버깅] 함수 진입 로그
        System.out.println("=== [GAME LOG] 탈락 처리 시작: 유저=" + username + ", 방=" + roomId);

        // 1. 게임 상태 조회 및 유효성 검사
        WordGameState state = wordGameStateService.getRawState(roomId);
        if (state == null) {
            System.err.println("=== [ERROR] 해당 방의 상태를 찾을 수 없습니다: " + roomId);
            return;
        }

        if (!state.isStarted()) {
            System.out.println("=== [GAME LOG] 이미 종료되었거나 시작되지 않은 게임입니다. (isStarted=false)");
            return;
        }

        // 2. 데이터 역직렬화 (JSON -> List)
        List<String> eliminated = parseList(state.getEliminatedPlayersJson());
        List<String> rankings = parseList(state.getRankingsJson());
        List<String> turnOrder = parseList(state.getTurnOrderJson());

        // 중복 탈락 방지
        if (eliminated.contains(username)) {
            System.out.println("=== [GAME LOG] 이미 탈락 처리된 유저: " + username);
            return;
        }

        // 3. 랭킹 리스트 초기화 (인원수만큼 공간 확보)
        if (rankings.isEmpty() && !turnOrder.isEmpty()) {
            rankings = new java.util.ArrayList<>(java.util.Collections.nCopies(turnOrder.size(), null));
        }

        // 4. 탈락자 추가 및 순위 저장 (뒤에서부터 채우기)
        eliminated.add(username);
        // 예: 4명 중 1번째 탈락자 발생 -> index = 4 - 1 = 3 (꼴등)
        int rankIndex = turnOrder.size() - eliminated.size();

        if (rankIndex >= 0 && rankIndex < rankings.size()) {
            rankings.set(rankIndex, username);
            System.out.println("=== [GAME LOG] " + username + "님 " + (rankIndex + 1) + "등 확정");
        }

        // 5. 생존자 계산
        List<String> survivors = turnOrder.stream()
                .filter(u -> !eliminated.contains(u))
                .toList();
        int survivorCount = survivors.size();
        System.out.println("=== [GAME LOG] 현재 생존자 수: " + survivorCount + "명 " + survivors);

        // 6. 게임 종료 여부 판단 (생존자가 1명 이하인 경우)
        if (survivorCount <= 1) {
            System.out.println("=== [GAME LOG] !!! 최종 생존자 발생, 게임 종료 !!!");

            // 마지막 생존자를 1등(인덱스 0)으로 설정
            if (survivorCount == 1) {
                String winner = survivors.get(0);
                rankings.set(0, winner);
                System.out.println("=== [GAME LOG] 최종 우승자(1등): " + winner);
            }

            // null 제거 (예기치 못한 상황 대비) 및 점수 정산
            rankings.removeAll(java.util.Collections.singleton(null));
            processScores(rankings);

            // 게임 상태 업데이트
            state.setStarted(false);

            // 종료 메시지 브로드캐스트
            WordChainMessage gameOverMsg = WordChainMessage.builder()
                    .type(WordChainMessage.MessageType.GAME_OVER)
                    .roomId(roomId)
                    .message("게임 종료! 최종 우승: " + (survivors.isEmpty() ? "없음" : survivors.get(0)))
                    .build();
            broadcastToRoom(roomId, gameOverMsg);
            messagingTemplate.convertAndSend("/topic/game/" + roomId + "/rankings", rankings);
        } else {
            // 게임 계속 진행 - 탈락 메시지만 전송
            WordChainMessage elimMsg = WordChainMessage.builder()
                    .type(WordChainMessage.MessageType.ELIMINATION)
                    .roomId(roomId)
                    .username(username)
                    .message(username + "님이 탈락했습니다!")
                    .build();
            broadcastToRoom(roomId, elimMsg);
        }

        // 7. 최종 상태 DB 저장
        state.setEliminatedPlayersJson(toJson(eliminated));
        state.setRankingsJson(toJson(rankings));
        wordGameStateService.saveState(state);
        System.out.println("=== [GAME LOG] 모든 상태 DB 저장 완료");
    }

    private void processScores(List<String> rankedUsers) {
        // rankedUsers: 0번 인덱스가 1등
        for (int i = 0; i < rankedUsers.size(); i++) {
            String user = rankedUsers.get(i);
            long points = 0;
            if (i == 0)
                points = 100; // 1등
            else if (i == 1)
                points = 50; // 2등
            else if (i == 2)
                points = 30; // 3등
            else
                points = 10; // 그 외

            try {
                userService.addPoints(user, points);
                log.info("[점수 지급 성공] 유저: {}, 순위: {}등, 지급 점수: {}점", user, (i + 1), points);
            } catch (Exception e) {
                log.error("[점수 지급 실패] 유저: {}, 이유: {}", user, e.getMessage());
            }
        }
    }

    private List<String> parseList(String json) {
        try {
            if (json == null || json.isEmpty())
                return new java.util.ArrayList<>();
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void broadcastToRoom(String roomId, WordChainMessage message) {
        messagingTemplate.convertAndSend("/topic/game/" + roomId, message);
    }

    public String getLastWord(String roomId) {
        return lastWordByRoom.get(roomId);
    }

    public void resetRoom(String roomId) {
        lastWordByRoom.remove(roomId);
        usedWordsByRoom.remove(roomId); // ✅ 추가
    }
}