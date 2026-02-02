package com.example.gitoo.controller;

import com.example.gitoo.model.WordChainMessage;
import com.example.gitoo.service.WordChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class WordChainController {

    @Autowired
    private WordChainService wordChainService;

    @MessageMapping("/join")
    @SendTo("/topic/game")
    public WordChainMessage joinGame(WordChainMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getUsername());

        message.setType(WordChainMessage.MessageType.JOIN);
        message.setMessage(message.getUsername() + "님이 게임에 참가했습니다.");

        return message;
    }

    @MessageMapping("/word")
    @SendTo("/topic/game")
    public WordChainMessage submitWord(WordChainMessage message) {
        String result = wordChainService.validateWord(message.getWord());

        if (result.equals("SUCCESS")) {
            message.setType(WordChainMessage.MessageType.WORD);
            message.setMessage(message.getUsername() + ": " + message.getWord());
            return message;
        } else {
            message.setType(WordChainMessage.MessageType.ERROR);
            message.setMessage(result);
            return message;
        }
    }

    @MessageMapping("/leave")
    @SendTo("/topic/game")
    public WordChainMessage leaveGame(WordChainMessage message) {
        message.setType(WordChainMessage.MessageType.LEAVE);
        message.setMessage(message.getUsername() + "님이 게임을 떠났습니다.");
        return message;
    }
}