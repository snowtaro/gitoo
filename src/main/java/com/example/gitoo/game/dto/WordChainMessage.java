package com.example.gitoo.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordChainMessage {
    private String username;
    private String word;
    private MessageType type;
    private String message;
    private String roomId;

    public enum MessageType {
        JOIN, // 게임 참가
        WORD, // 단어 제출
        LEAVE, // 게임 떠나기
        ERROR, // 에러 메시지
        GAME_STATE, // 게임 상태
        GAME_STARTED, // 게임 시작
        ELIMINATION, // 탈락
        GAME_OVER // 게임 종료
    }
}