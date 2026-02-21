package com.example.gitoo.service;

import com.example.gitoo.model.WordChainMessage;
import com.example.gitoo.repository.WordGameStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WordChainService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WordGameStateRepository wordGameStateRepository;

    @Autowired
    private WordGameStateService wordGameStateService;

    // 마지막으로 사용된 단어를 방별로 저장
    private final Map<String, String> lastWordByRoom = new ConcurrentHashMap<>();

    /**
     * 단어 검증 (기존 메서드)
     */
    public String validateWord(String word, String roomId) {
        // 1. 빈 단어 체크
        if (word == null || word.trim().isEmpty()) {
            return "단어를 입력해주세요.";
        }

        // 2. 한글만 허용
        if (!word.matches("^[가-힣]+$")) {
            return "한글만 입력 가능합니다.";
        }

        // 3. 끝말잇기 규칙 체크
        String lastWord = lastWordByRoom.get(roomId);
        if (lastWord != null) {
            char lastChar = lastWord.charAt(lastWord.length() - 1);
            char firstChar = word.charAt(0);

            if (lastChar != firstChar) {
                return "'" + lastChar + "'(으)로 시작하는 단어를 입력해주세요.";
            }
        }

        // 4. 사전 검증 (여기에 실제 사전 API 연동 가능)
        // if (!dictionaryService.exists(word)) {
        //     return "사전에 없는 단어입니다.";
        // }

        return "SUCCESS";
    }

    public void handleWord(WordChainMessage message) {
        String roomId = message.getRoomId();
        String word = message.getWord();

        String validationResult = validateWord(word, roomId);

        if ("SUCCESS".equals(validationResult)) {
            lastWordByRoom.put(roomId, word);

            wordGameStateRepository.findById(roomId).ifPresent(state -> {
                List<String> order = wordGameStateService.readTurnOrder(state.getTurnOrderJson());
                int nextIndex = (state.getTurnIndex() + 1) % order.size();
                String nextTurn = order.get(nextIndex);

                state.setLastWord(word);
                state.setTurnStartedAt(Instant.now());
                state.setTurnIndex(nextIndex);
                state.setCurrentTurn(nextTurn);
                wordGameStateRepository.save(state);
            });

            message.setType(WordChainMessage.MessageType.WORD);
            message.setMessage(message.getUsername() + ": " + word);
        } else {
            message.setType(WordChainMessage.MessageType.ERROR);
            message.setMessage(validationResult);
        }

        broadcastToRoom(roomId, message);
    }

    /**
     * 게임 참가 처리
     */
    public void handleJoin(WordChainMessage message) {
        message.setType(WordChainMessage.MessageType.JOIN);
        message.setMessage(message.getUsername() + "님이 게임에 참가했습니다.");

        broadcastToRoom(message.getRoomId(), message);
    }

    /**
     * 게임 떠나기 처리
     */
    public void handleLeave(WordChainMessage message) {
        message.setType(WordChainMessage.MessageType.LEAVE);
        message.setMessage(message.getUsername() + "님이 게임을 떠났습니다.");

        // 마지막 단어 초기화 (선택사항)
        // lastWordByRoom.remove(message.getRoomId());

        broadcastToRoom(message.getRoomId(), message);
    }

    /**
     * 특정 방으로 메시지 브로드캐스트
     */
    private void broadcastToRoom(String roomId, WordChainMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                message
        );
    }

    /**
     * 방의 마지막 단어 조회
     */
    public String getLastWord(String roomId) {
        return lastWordByRoom.get(roomId);
    }

    /**
     * 방 초기화 (게임 종료 시)
     */
    public void resetRoom(String roomId) {
        lastWordByRoom.remove(roomId);
    }
}