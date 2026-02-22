package com.example.gitoo.game.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "word_game_states")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WordGameState {

    /**
     * roomId (PK) : 방당 게임 상태 1개
     */
    @Id
    @Column(name = "room_id", length = 36)
    private String roomId;

    /**
     * 게임 시작 여부
     */
    @Column(nullable = false)
    private boolean started;

    /**
     * 턴 순서 JSON 문자열 (예: ["강기영","철수","영희"])
     * - DB 종류 상관없이 가장 안정적인 방식
     */
    @Lob
    @Column(name = "turn_order_json", nullable = false)
    private String turnOrderJson;

    /**
     * 현재 턴 인덱스 (turnOrder의 몇 번째인지)
     */
    @Column(name = "turn_index", nullable = false)
    private int turnIndex;

    /**
     * 현재 턴 닉네임 (편의용: turnOrder[turnIndex]와 동일)
     */
    @Column(name = "current_turn", length = 50)
    private String currentTurn;

    /**
     * 마지막으로 정상 처리된 단어
     */
    @Column(name = "last_word", length = 50)
    private String lastWord;

    /**
     * 현재 턴이 시작된 시각 (타이머/시간초과 복구용)
     */
    @Column(name = "turn_started_at")
    private Instant turnStartedAt;

    /**
     * 탈락자 목록 (닉네임) - JSON
     */
    @Lob
    @Column(name = "eliminated_players_json")
    private String eliminatedPlayersJson;

    /**
     * 순위 목록 (닉네임) - JSON (1등, 2등 ... 순)
     */
    @Lob
    @Column(name = "rankings_json")
    private String rankingsJson;

    /**
     * 마지막 업데이트 시각
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null)
            updatedAt = Instant.now();
        // started 기본 false
        // turnOrderJson은 nullable=false라서 저장 전에 반드시 세팅해야 함
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
