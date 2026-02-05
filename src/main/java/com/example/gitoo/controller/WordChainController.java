package com.example.gitoo.controller;

import com.example.gitoo.model.WordChainMessage;
import com.example.gitoo.service.WordChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class WordChainController {

    @Autowired
    private WordChainService wordChainService;

    @MessageMapping("/join")
    public void joinGame(WordChainMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getUsername());
        wordChainService.handleJoin(message);
    }

    @MessageMapping("/word")
    public void submitWord(WordChainMessage message) {
        wordChainService.handleWord(message);
    }

    @MessageMapping("/leave")
    public void leaveGame(WordChainMessage message) {
        wordChainService.handleLeave(message);
    }
}