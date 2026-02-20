//package com.example.gitoo.controller;
//
//import com.example.gitoo.game.WordChainService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/game")
//@CrossOrigin(origins = "*")
//public class GameRestController {
//
//    @Autowired
//    private WordChainService wordChainService;
//
//    @GetMapping("/status")
//    public Map<String, Object> getGameStatus() {
//        Map<String, Object> status = new HashMap<>();
//        status.put("lastWord", wordChainService.getLastWord());
//        status.put("usedWordsCount", wordChainService.getUsedWordsCount());
//        return status;
//    }
//
//    @PostMapping("/reset")
//    public Map<String, String> resetGame() {
//        wordChainService.resetGame();
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "게임이 초기화되었습니다.");
//        return response;
//    }
//}