package com.example.connectifychattingapp;

public class MessageModel {
    String userId , message;
    Long timestamp;

    public MessageModel(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public MessageModel(){}

    public String getuserId() {
        return userId;
    }

    public void setuserId(String uId) {
        this.userId = uId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageModel(String userId, String message, Long timestamp) {
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;




    }
}
