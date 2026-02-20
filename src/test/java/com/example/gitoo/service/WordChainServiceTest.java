package com.example.gitoo.service;

import com.example.gitoo.game.WordChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordChainServiceTest {

    private WordChainService wordChainService;

    @BeforeEach
    void setUp() {
        wordChainService = new WordChainService();
    }

    @Test
    void testFirstWord() {
        String result = wordChainService.validateWord("사과");
        assertEquals("SUCCESS", result);
    }

    @Test
    void testValidWordChain() {
        wordChainService.validateWord("사과");
        String result = wordChainService.validateWord("과일");
        assertEquals("SUCCESS", result);
    }

    @Test
    void testInvalidWordChain() {
        wordChainService.validateWord("사과");
        String result = wordChainService.validateWord("바나나");
        assertTrue(result.contains("'과'"));
    }

    @Test
    void testDuplicateWord() {
        wordChainService.validateWord("사과");
        wordChainService.validateWord("과일");
        String result = wordChainService.validateWord("사과");
        assertEquals("이미 사용된 단어입니다.", result);
    }

    @Test
    void testShortWord() {
        String result = wordChainService.validateWord("사");
        assertEquals("두 글자 이상 입력해주세요.", result);
    }

    @Test
    void testNonKoreanWord() {
        String result = wordChainService.validateWord("apple");
        assertEquals("한글만 입력 가능합니다.", result);
    }

    @Test
    void testEmptyWord() {
        String result = wordChainService.validateWord("");
        assertEquals("단어를 입력해주세요.", result);
    }

    @Test
    void testResetGame() {
        wordChainService.validateWord("사과");
        wordChainService.resetGame();
        assertEquals("", wordChainService.getLastWord());
        assertEquals(0, wordChainService.getUsedWordsCount());
    }
}