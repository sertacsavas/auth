package com.sertac.ai.model.dto;

public class SendVerificationCodeResponse {
    private String message;
    private boolean success;
    public SendVerificationCodeResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public SendVerificationCodeResponse() {
        super();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
