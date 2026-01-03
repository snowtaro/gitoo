package com.example.gitoo.model;

public class WordChainMessage {
    private String username;
    private String word;
    private MessageType type;
    private String message;

    public enum MessageType {
        JOIN, WORD, LEAVE, ERROR, GAME_STATE
    }

    public WordChainMessage() {}

    public WordChainMessage(String username, String word, MessageType type) {
        this.username = username;
        this.word = word;
        this.type = type;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}