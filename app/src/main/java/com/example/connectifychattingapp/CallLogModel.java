package com.example.connectifychattingapp;

public class CallLogModel {
    private String userId, userName, profilePic, callType, callStatus, callId;
    private long timestamp;

    public CallLogModel() {} // Required for Firebase

    public CallLogModel(String userId, String userName, String profilePic, String callType, String callStatus, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.profilePic = profilePic;
        this.callType = callType;
        this.callStatus = callStatus;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
}