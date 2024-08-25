package com.sertac.ai.service;

import com.sertac.ai.model.dto.VerifyCodeResponse;
import com.sertac.ai.model.dto.SendVerificationCodeRequest;
import com.sertac.ai.model.dto.VerifyCodeRequest;
import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.exception.VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;

class AuthServiceTest {

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private UserService userService;

    private static final String SECRET_KEY = "yourVeryLongAndSecureSecretKeyHere";
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(verificationCodeService, emailSender, SECRET_KEY,userService);
    }

    @Test
    void sendVerificationCode_ShouldGenerateAndSendCode() {
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeService.generateVerificationCode()).thenReturn(code);

        SendVerificationCodeRequest request = new SendVerificationCodeRequest(email);
        authService.sendVerificationCode(request);

        verify(verificationCodeService).generateVerificationCode();
        verify(verificationCodeService).saveVerificationCode(any(VerificationCode.class));
        verify(emailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void verifyCode_WithValidCode_ShouldReturnAuthenticationResponse() {
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeService.verifyCode(email, code)).thenReturn(true);

        VerifyCodeRequest request = new VerifyCodeRequest(email, code);
        VerifyCodeResponse response = authService.verifyCode(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(verificationCodeService).deactivateVerificationCode(email);
    }

    @Test
    void verifyCode_WithInvalidCode_ShouldThrowVerificationException() {
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeService.verifyCode(email, code)).thenReturn(false);

        assertThrows(VerificationException.class, () -> {
            VerifyCodeRequest request = new VerifyCodeRequest(email, code);
            authService.verifyCode(request);
        });
    }

    @Test
    void verifyCode_ShouldReturnValidJwtToken() {
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeService.verifyCode(email, code)).thenReturn(true);

        VerifyCodeRequest request = new VerifyCodeRequest(email, code);
        VerifyCodeResponse response = authService.verifyCode(request);

        assertNotNull(response);
        assertNotNull(response.getToken());

        // Verify the JWT token
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(response.getToken())
                .getBody();

        assertEquals(email, claims.getSubject());
        assertTrue(claims.getExpiration().after(new Date()));
        verify(verificationCodeService).deactivateVerificationCode(email);
    }
}