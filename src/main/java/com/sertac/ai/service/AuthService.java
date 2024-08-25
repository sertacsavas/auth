package com.sertac.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sertac.ai.model.dto.VerifyCodeResponse;
import com.sertac.ai.model.dto.RefreshTokenRequest;
import com.sertac.ai.model.dto.RefreshTokenResponse;
import com.sertac.ai.model.dto.SendVerificationCodeRequest;
import com.sertac.ai.model.dto.SendVerificationCodeResponse;
import com.sertac.ai.model.dto.VerifyCodeRequest;
import com.sertac.ai.model.entity.User;
import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.enums.RefreshTokenStatus;
import com.sertac.ai.model.entity.RefreshToken;
import com.sertac.ai.model.exception.AuthenticationException;
import com.sertac.ai.model.exception.EmailSendingException;
import com.sertac.ai.model.exception.TooManyRequestsException;
import com.sertac.ai.model.exception.VerificationException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class AuthService {
    private final VerificationCodeService verificationCodeService;
    private final JavaMailSender emailSender;
    private final String secretKey;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    @Value("${app.domain}")
    private String appDomain;

    public AuthService(VerificationCodeService verificationCodeService, 
                       JavaMailSender emailSender, 
                       @Value("${auth.secret-key}") String secretKey,
                       UserService userService,
                       RefreshTokenService refreshTokenService) {
        this.verificationCodeService = verificationCodeService;
        this.emailSender = emailSender;
        this.secretKey = secretKey;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }
    
    
    public SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request) {
        // Check for recent active verification codes, excluding USED ones
        if (verificationCodeService.hasRecentActiveVerificationCode(request.getEmail())) {
            throw new TooManyRequestsException("Please wait before requesting a new code");
        }
        
        // Deactivate any existing verification codes
        verificationCodeService.deactivateVerificationCode(request.getEmail());
        
        // Generate and save new verification code
        String code = verificationCodeService.generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode(request.getEmail(), code);
        verificationCodeService.saveVerificationCode(verificationCode);
        
        String encodedEmail = URLEncoder.encode(request.getEmail(), StandardCharsets.UTF_8);
        String loginUrl = "http://" + appDomain + "/verify?email=" + encodedEmail;
        String emailBody = String.format(
            "Your verification code is: %s\n\n" +
            "Please use this code to verify your email at: %s\n\n" +
            "If you didn't request this code, please ignore this email.",
            code, loginUrl
        );
        
        sendEmail(request.getEmail(), "Verification Code for " + appDomain, emailBody);
        return new SendVerificationCodeResponse(true, "Verification code sent successfully");
    }
    
    public VerifyCodeResponse verifyCode(VerifyCodeRequest verifyCodeRequest) {
        if (verificationCodeService.verifyCode(verifyCodeRequest.getEmail(), verifyCodeRequest.getCode())) {
            // Create a new user
            User existingUser = userService.findByEmail(verifyCodeRequest.getEmail());
            if (existingUser == null) {
                userService.createUser(new User(verifyCodeRequest.getEmail()));
            }
            
            String accessToken = createJwtToken(verifyCodeRequest.getEmail());
            String refreshToken = createRefreshToken(verifyCodeRequest.getEmail());
            
            return new VerifyCodeResponse(accessToken, refreshToken);
        } else {
            throw new VerificationException("Invalid verification code");
        }
    }
    
    private String createJwtToken(String email) {
        long expirationTime = 1000 * 60 * 60 * 24; // 24 hours
        Date expirationDate = new Date(System.currentTimeMillis() + expirationTime);
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    private String createRefreshToken(String email) {
        String tokenId = UUID.randomUUID().toString();
        long expirationTime = 1000 * 60 * 60 * 24 * 30; // 30 days
        Date expirationDate = new Date(System.currentTimeMillis() + expirationTime);
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());

        String token = Jwts.builder()
                .setId(tokenId)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        refreshTokenService.saveRefreshToken(tokenId, email, token, expirationDate);

        return token;
    }
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
        } catch (MailException e) {
            // Log the error
            throw new EmailSendingException("Failed to send verification email", e);
        }
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenString = request.getRefreshToken();
        RefreshToken refreshToken = validateAndGetRefreshToken(refreshTokenString);
        String email = refreshToken.getEmail();
        getUserOrThrow(email);

        String newAccessToken = createJwtToken(email);
        String newRefreshToken = createRefreshToken(email);

        refreshTokenService.deactivateRefreshToken(refreshToken);
        refreshTokenService.blacklistToken(refreshTokenString);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    private RefreshToken validateAndGetRefreshToken(String refreshTokenString) {
        if (refreshTokenString == null || refreshTokenString.isEmpty()) {
            throw new AuthenticationException("Refresh token is missing or empty");
        }

        if (refreshTokenService.isTokenBlacklisted(refreshTokenString)) {
            throw new AuthenticationException("Refresh token is blacklisted");
        }

        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new AuthenticationException("Refresh token not found"));

        if (!RefreshTokenStatus.ACTIVE.equals(refreshToken.getStatus())) {
            throw new AuthenticationException("Refresh token is inactive");
        }

        if (refreshToken.getExpiryDate().before(new Date())) {
            refreshTokenService.deactivateRefreshToken(refreshToken);
            throw new AuthenticationException("Refresh token has expired");
        }

        try {
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshTokenString);
        } catch (JwtException e) {
            refreshTokenService.deactivateRefreshToken(refreshToken);
            throw new AuthenticationException("Invalid refresh token", e);
        }

        return refreshToken;
    }

    private User getUserOrThrow(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("User not found");
        }
        return user;
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
        refreshTokenService.blacklistToken(refreshToken);
    }

    public void revokeAllUserRefreshTokens(String email) {
        List<RefreshToken> userTokens = refreshTokenService.findActiveTokensByEmail(email);
        for (RefreshToken token : userTokens) {
            refreshTokenService.deactivateRefreshToken(token);
            refreshTokenService.blacklistToken(token.getToken());
        }
    }
}