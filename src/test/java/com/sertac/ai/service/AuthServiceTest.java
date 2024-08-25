package com.sertac.ai.service;

import com.sertac.ai.model.dto.RefreshTokenRequest;
import com.sertac.ai.model.dto.RefreshTokenResponse;
import com.sertac.ai.model.dto.SendVerificationCodeRequest;
import com.sertac.ai.model.dto.SendVerificationCodeResponse;
import com.sertac.ai.model.dto.VerifyCodeRequest;
import com.sertac.ai.model.dto.VerifyCodeResponse;
import com.sertac.ai.model.entity.RefreshToken;
import com.sertac.ai.model.entity.User;
import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.enums.RefreshTokenStatus;
import com.sertac.ai.model.exception.AuthenticationException;
import com.sertac.ai.model.exception.TooManyRequestsException;
import com.sertac.ai.model.exception.VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

class AuthServiceTest {

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private static final String SECRET_KEY = "yourVeryLongAndSecureSecretKeyHere";
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(verificationCodeService, emailSender, SECRET_KEY,userService,refreshTokenService);
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

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(messageCaptor.capture());
        
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String[] recipients = sentMessage.getTo();
        assertNotNull(recipients);
        assertEquals(email, recipients[0]);
        assertNotNull(sentMessage.getText());
        String messageText = sentMessage.getText();
        assertNotNull(messageText);
        assertTrue(messageText.contains(code));
    }

    @Test
    void verifyCode_WithValidCode_ShouldReturnAuthenticationResponse() {
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeService.verifyCode(email, code)).thenReturn(true);
        when(userService.findByEmail(email)).thenReturn(new User(email));

        VerifyCodeRequest request = new VerifyCodeRequest(email, code);
        VerifyCodeResponse response = authService.verifyCode(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(verificationCodeService).verifyCode(email, code);
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
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        // Verify the JWT token
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(response.getAccessToken())
                .getBody();

        assertEquals(email, claims.getSubject());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void sendVerificationCode_Success() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest("test@example.com");
        when(verificationCodeService.hasRecentActiveVerificationCode(anyString())).thenReturn(false);
        when(verificationCodeService.generateVerificationCode()).thenReturn("123456");

        SendVerificationCodeResponse response = authService.sendVerificationCode(request);

        assertTrue(response.isSuccess());
        assertEquals("Verification code sent successfully", response.getMessage());
        verify(verificationCodeService).saveVerificationCode(any(VerificationCode.class));
        verify(emailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendVerificationCode_TooManyRequests() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest("test@example.com");
        when(verificationCodeService.hasRecentActiveVerificationCode(anyString())).thenReturn(true);

        assertThrows(TooManyRequestsException.class, () -> authService.sendVerificationCode(request));
    }

    @Test
    void verifyCode_Success_NewUser() {
        VerifyCodeRequest request = new VerifyCodeRequest("test@example.com", "123456");
        when(verificationCodeService.verifyCode(anyString(), anyString())).thenReturn(true);
        when(userService.findByEmail(anyString())).thenReturn(null);

        VerifyCodeResponse response = authService.verifyCode(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(userService).createUser(any(User.class));
    }

    @Test
    void verifyCode_Success_ExistingUser() {
        VerifyCodeRequest request = new VerifyCodeRequest("test@example.com", "123456");
        when(verificationCodeService.verifyCode(anyString(), anyString())).thenReturn(true);
        when(userService.findByEmail(anyString())).thenReturn(new User("test@example.com"));

        VerifyCodeResponse response = authService.verifyCode(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void verifyCode_InvalidCode() {
        VerifyCodeRequest request = new VerifyCodeRequest("test@example.com", "123456");
        when(verificationCodeService.verifyCode(anyString(), anyString())).thenReturn(false);

        assertThrows(VerificationException.class, () -> authService.verifyCode(request));
    }

    @Test
    void refreshToken_Success() {
        // Generate a valid JWT refresh token
        String validRefreshToken = Jwts.builder()
            .setSubject("test@example.com")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
            .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
            .compact();

        RefreshTokenRequest request = new RefreshTokenRequest(validRefreshToken);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEmail("test@example.com");
        refreshToken.setToken(validRefreshToken);
        refreshToken.setStatus(RefreshTokenStatus.ACTIVE);
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + 3600000));

        when(refreshTokenService.findByToken(validRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(userService.findByEmail("test@example.com")).thenReturn(new User("test@example.com"));

        RefreshTokenResponse response = authService.refreshToken(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenService).deactivateRefreshToken(any(RefreshToken.class));
        verify(refreshTokenService).blacklistToken(anyString());
    }

    @Test
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalidRefreshToken");
        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_ExpiredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("expiredRefreshToken");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEmail("test@example.com");
        refreshToken.setStatus(RefreshTokenStatus.ACTIVE);
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() - 3600000));

        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.of(refreshToken));

        assertThrows(AuthenticationException.class, () -> authService.refreshToken(request));
        verify(refreshTokenService).deactivateRefreshToken(any(RefreshToken.class));
    }

    @Test
    void revokeRefreshToken() {
        String refreshToken = "tokenToRevoke";
        authService.revokeRefreshToken(refreshToken);

        verify(refreshTokenService).revokeRefreshToken(refreshToken);
        verify(refreshTokenService).blacklistToken(refreshToken);
    }

    @Test
    void revokeAllUserRefreshTokens() {
        String email = "test@example.com";
        RefreshToken token1 = new RefreshToken();
        token1.setToken("token1");
        RefreshToken token2 = new RefreshToken();
        token2.setToken("token2");
        List<RefreshToken> userTokens = Arrays.asList(token1, token2);

        when(refreshTokenService.findActiveTokensByEmail(email)).thenReturn(userTokens);

        authService.revokeAllUserRefreshTokens(email);

        verify(refreshTokenService, times(2)).deactivateRefreshToken(any(RefreshToken.class));
        verify(refreshTokenService, times(2)).blacklistToken(anyString());
    }
}