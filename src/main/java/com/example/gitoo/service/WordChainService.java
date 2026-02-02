package com.example.gitoo.service;

import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class WordChainService {

    private String lastWord = "";
    private Set<String> usedWords = new HashSet<>();

    public String validateWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return "단어를 입력해주세요.";
        }

        word = word.trim();

        // 한글만 허용
        if (!word.matches("^[가-힣]+$")) {
            return "한글만 입력 가능합니다.";
        }

        // 두 글자 이상
        if (word.length() < 2) {
            return "두 글자 이상 입력해주세요.";
        }

        // 이미 사용된 단어인지 확인
        if (usedWords.contains(word)) {
            return "이미 사용된 단어입니다.";
        }

        // 첫 단어가 아닌 경우, 끝말잇기 규칙 확인
        if (!lastWord.isEmpty()) {
            char lastChar = lastWord.charAt(lastWord.length() - 1);
            char firstChar = word.charAt(0);

            if (lastChar != firstChar) {
                return "'" + lastChar + "'(으)로 시작하는 단어를 입력해주세요.";
            }
        }

        // 유효한 단어
        usedWords.add(word);
        lastWord = word;
        return "SUCCESS";
    }

    public String getLastWord() {
        return lastWord;
    }

    public int getUsedWordsCount() {
        return usedWords.size();
    }

    public void resetGame() {
        lastWord = "";
        usedWords.clear();
    }
}