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
import com.sertac.ai.model.exception.AuthenticationException;
import com.sertac.ai.model.exception.EmailSendingException;
import com.sertac.ai.model.exception.TooManyRequestsException;
import com.sertac.ai.model.exception.VerificationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Service
@Transactional
public class AuthService {
    private final VerificationCodeService verificationCodeService;
    private final JavaMailSender emailSender;
    private final String secretKey;
    private final UserService userService;

    public AuthService(VerificationCodeService verificationCodeService, 
                       JavaMailSender emailSender, 
                       @Value("${auth.secret-key}") String secretKey,
                       UserService userService) {
        this.verificationCodeService = verificationCodeService;
        this.emailSender = emailSender;
        this.secretKey = secretKey;
        this.userService = userService;
    }
    
    
    public SendVerificationCodeResponse sendVerificationCode(SendVerificationCodeRequest request) {
        if (verificationCodeService.hasRecentVerificationCode(request.getEmail())) {
            throw new TooManyRequestsException("Please wait before requesting a new code");
        }
        verificationCodeService.deactivateVerificationCode(request.getEmail());
        String code = verificationCodeService.generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode(request.getEmail(), code);
        verificationCodeService.saveVerificationCode(verificationCode);
        sendEmail(request.getEmail(), "Verification Code", "Your verification code is: " + code);
        return new SendVerificationCodeResponse(true, "Verification code sent successfully");
    }
    
    public VerifyCodeResponse verifyCode(VerifyCodeRequest verifyCodeRequest) {
        if (verificationCodeService.verifyCode(verifyCodeRequest.getEmail(), verifyCodeRequest.getCode())) {
            // Create a new user
            User existingUser = userService.findByEmail(verifyCodeRequest.getEmail());
            if (existingUser == null) {
                userService.createUser(new User(verifyCodeRequest.getEmail()));
            }
            
            String token = createJwtToken(verifyCodeRequest.getEmail());
            verificationCodeService.deactivateVerificationCode(verifyCodeRequest.getEmail());
            return new VerifyCodeResponse(token);
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
        validateRefreshToken(request.getRefreshToken());
        String email = decodeRefreshToken(request.getRefreshToken());
        getUserOrThrow(email);
        String newAccessToken = createJwtToken(email);
        return new RefreshTokenResponse(newAccessToken);
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AuthenticationException("Refresh token is missing or empty");
        }
        try {
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken);
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid refresh token", e);
        }
    }

    private User getUserOrThrow(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("User not found");
        }
        return user;
    }

    private String decodeRefreshToken(String refreshToken) {
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
        String email = claims.getSubject();
        if (email == null || email.isEmpty()) {
            throw new AuthenticationException("Email not found in refresh token");
        }
        return email;

    }
}