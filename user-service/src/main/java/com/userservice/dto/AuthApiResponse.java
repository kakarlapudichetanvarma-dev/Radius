package com.userservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthApiResponse {

    private boolean success;
    private String message;
    private AuthUserResponse data;

    public AuthApiResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AuthUserResponse getData() {
        return data;
    }

    public void setData(AuthUserResponse data) {
        this.data = data;
    }
}