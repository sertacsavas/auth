package com.sertac.ai.controller;

import com.sertac.ai.model.dto.*;
import com.sertac.ai.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendVerificationCode_shouldReturnOk() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        SendVerificationCodeResponse expectedResponse = new SendVerificationCodeResponse();
        when(authService.sendVerificationCode(request)).thenReturn(expectedResponse);

        ResponseEntity<SendVerificationCodeResponse> response = authController.sendVerificationCode(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).sendVerificationCode(request);
    }

    @Test
    void verifyCode_shouldReturnOk() {
        VerifyCodeRequest request = new VerifyCodeRequest();
        VerifyCodeResponse expectedResponse = new VerifyCodeResponse();
        when(authService.verifyCode(request)).thenReturn(expectedResponse);

        ResponseEntity<VerifyCodeResponse> response = authController.verifyCode(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).verifyCode(request);
    }

    @Test
    void refreshToken_shouldReturnOk() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        RefreshTokenResponse expectedResponse = new RefreshTokenResponse();
        when(authService.refreshToken(request)).thenReturn(expectedResponse);

        ResponseEntity<RefreshTokenResponse> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).refreshToken(request);
    }
}