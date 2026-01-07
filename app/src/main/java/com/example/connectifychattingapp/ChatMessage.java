package com.example.connectifychattingapp;
public class ChatMessage {
    private String message;
    private boolean isUser; // true for User, false for Gemini
    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
    public String getMessage(){
        return message;
    }
    public boolean isUser(){
        return isUser;
    }
}