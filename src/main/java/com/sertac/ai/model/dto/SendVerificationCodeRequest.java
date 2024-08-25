package com.sertac.ai.model.dto;

public class SendVerificationCodeRequest {
    private String email;

    public SendVerificationCodeRequest(String email) {
        this.email = email;
    }

    public SendVerificationCodeRequest() {
        super();
    }
    // Getter and setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
