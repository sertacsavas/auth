package com.sertac.ai.model.dto;

public class VerifyCodeResponse {
    private final String token;

    public VerifyCodeResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}