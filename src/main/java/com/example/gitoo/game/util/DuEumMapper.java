package com.example.gitoo.game.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DuEumMapper {

    // 두음법칙: 단어 첫 글자의 초성이 ㄹ 또는 ㄴ일 때 변환 가능한 초성 쌍
    // key: 원래 초성 index, value: 두음법칙 적용 초성 index
    // 초성 순서: ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ (index 0~18)
    private static final Map<Integer, Integer> DU_EUM_MAP = new HashMap<>();

    static {
        // ㄹ(5) → ㄴ(2) 또는 ㅇ(11)
        DU_EUM_MAP.put(5, 2);   // ㄹ → ㄴ
        DU_EUM_MAP.put(5, 11);  // ㄹ → ㅇ  ← 아래 getAlternatives로 처리
        // ㄴ(2) → ㅇ(11)
        DU_EUM_MAP.put(2, 11);  // ㄴ → ㅇ
    }

    // 초성 리스트
    private static final int[] CHOSUNG = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
    private static final int CHOSUNG_COUNT = 19;
    private static final int JUNGSUNG_COUNT = 21;
    private static final int JONGSUNG_COUNT = 28;

    /**
     * 한글 글자의 초성 인덱스를 반환 (0~18), 한글이 아니면 -1
     */
    public static int getChosungIndex(char c) {
        if (c < '가' || c > '힣') return -1;
        int code = c - '가';
        return code / (JUNGSUNG_COUNT * JONGSUNG_COUNT);
    }

    /**
     * 두음법칙을 고려하여, lastChar의 마지막 글자와 firstChar가 이어질 수 있는지 확인
     */
    public static boolean isValidConnection(char lastChar, char firstChar) {
        if (lastChar == firstChar) return true;

        int lastChosung = getChosungIndex(lastChar);
        int firstChosung = getChosungIndex(firstChar);

        if (lastChosung < 0 || firstChosung < 0) return false;

        // lastChar의 끝글자 초성과 firstChar의 초성이 두음법칙으로 연결 가능한지
        // 단, 비교 대상은 끝 글자 자체가 아닌 "끝 글자를 첫 글자로 쓸 때"의 두음법칙
        // → lastChar 초성의 두음법칙 대체 초성 집합에 firstChosung이 포함되는지 체크
        Set<Integer> alternatives = getAlternatives(lastChosung);
        return alternatives.contains(firstChosung);
    }

    /**
     * 특정 초성에 대해 두음법칙으로 허용되는 초성 집합 반환
     */
    public static Set<Integer> getAlternatives(int chosungIdx) {
        Set<Integer> result = new java.util.HashSet<>();
        result.add(chosungIdx); // 자기 자신은 항상 허용

        if (chosungIdx == 5) {       // ㄹ → ㄴ, ㅇ 허용
            result.add(2);
            result.add(11);
        } else if (chosungIdx == 2) { // ㄴ → ㅇ 허용
            result.add(11);
        }
        return result;
    }
}