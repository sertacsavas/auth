package com.sertac.ai.model.dto;

public class VerifyCodeResponse {
    private String accessToken;
    private String refreshToken;

    public VerifyCodeResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}