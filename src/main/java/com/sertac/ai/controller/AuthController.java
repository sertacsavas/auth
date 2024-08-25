package com.sertac.ai.controller;

import com.sertac.ai.model.dto.VerifyCodeResponse;
import com.sertac.ai.model.dto.SendVerificationCodeRequest;
import com.sertac.ai.model.dto.SendVerificationCodeResponse;
import com.sertac.ai.model.dto.VerifyCodeRequest;
import com.sertac.ai.model.dto.RefreshTokenRequest;
import com.sertac.ai.model.dto.RefreshTokenResponse;
import com.sertac.ai.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-verification")
    public ResponseEntity<SendVerificationCodeResponse> sendVerificationCode(@RequestBody SendVerificationCodeRequest request) {
        SendVerificationCodeResponse response = authService.sendVerificationCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-code")
    public ResponseEntity<VerifyCodeResponse> verifyCode(@RequestBody VerifyCodeRequest request) {
        VerifyCodeResponse result = authService.verifyCode(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
