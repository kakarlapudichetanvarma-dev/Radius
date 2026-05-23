package com.userservice.dto;

import jakarta.validation.constraints.*;

public class UserDtos {

    // ── Requests ──────────────────────────────────────────────────

    public static class FriendRequestDto {

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{6,14}$",
                message = "Invalid phone number format"
        )
        private String phoneNumber;

        public FriendRequestDto() {
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class FriendRequestActionDto {

        @NotNull(message = "requestId is required")
        private String requestId;

        @NotBlank(message = "action is required")
        @Pattern(
                regexp = "^(ACCEPT|REJECT)$",
                message = "action must be ACCEPT or REJECT"
        )
        private String action;

        public FriendRequestActionDto() {
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    // ── Responses ─────────────────────────────────────────────────

    public static class FriendSummaryResponse {

        private String userId;
        private String username;
        private String email;
        private String profilePicture;
        private String friendsSince;
        private String phoneNumber;

        public FriendSummaryResponse() {}

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

        public String getFriendsSince() { return friendsSince; }
        public void setFriendsSince(String friendsSince) { this.friendsSince = friendsSince; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    public static class FriendRequestResponse {

        private String requestId;
        private String requesterId;
        private String requesterUsername;
        private String email;
        private String phoneNumber;
        private String profilePicture;
        private String status;
        private String createdAt;

        public FriendRequestResponse() {
        }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getRequesterId() { return requesterId; }
        public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

        public String getRequesterUsername() { return requesterUsername; }
        public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ── NEW: returned by GET /friends/search-by-phone ─────────────────────────

    public static class UserSearchResult {

        private String userId;
        private String username;
        private String phoneNumber;
        private String profilePicture;
        private boolean alreadyFriend;

        public UserSearchResult() {}

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

        public boolean isAlreadyFriend() { return alreadyFriend; }
        public void setAlreadyFriend(boolean alreadyFriend) { this.alreadyFriend = alreadyFriend; }
    }

    public static class ApiResponse {

        private boolean success;
        private String message;
        private Object data;

        public ApiResponse() {
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    public static class ErrorResponse {

        private int status;
        private String error;
        private String message;
        private String path;
        private String timestamp;

        public ErrorResponse() {
        }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}