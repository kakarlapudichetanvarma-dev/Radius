package com.authservice.dto;

public class AvatarUpdateMessage {
    private String userId;
    private String profilePicture;

    public AvatarUpdateMessage(String userId, String profilePicture) {
        this.userId = userId;
        this.profilePicture = profilePicture;
    }

    public String getUserId() { return userId; }
    public String getProfilePicture() { return profilePicture; }
}