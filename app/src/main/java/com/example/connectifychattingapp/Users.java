package com.example.connectifychattingapp;
public class Users {
    String profilePic ,username,mail,password,userId,lastMessage;
    private boolean isSelected = false;
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Users(String profilePic, String username, String mail, String password, String userId, String lastMessage) {
        this.profilePic = profilePic;
        this.username = username;
        this.mail = mail;
        this.password = password;
        this.userId = userId;
        this.lastMessage = lastMessage;
    }
    public Users(){}

    //SignUp Constructor
    public Users( String username, String mail, String password) {
        this.username = username;
        this.mail = mail;
        this.password = password;

    }
    public String getProfilePic() {
        return profilePic;
    }
    public boolean isSelected() {
        return isSelected;
    }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
    public String getusername() {
        return username;
    }
    public void setusername(String username) {

        this.username = username;
    }
    public String getMail() {
        return mail;
    }
    public void setMail(String mail) {
        this.mail = mail;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getLastMessage() {
        return lastMessage;
    }
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
