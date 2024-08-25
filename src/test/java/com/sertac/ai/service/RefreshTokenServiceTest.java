package com.sertac.ai.service;

import com.sertac.ai.model.entity.RefreshToken;
import com.sertac.ai.model.enums.RefreshTokenStatus;
import com.sertac.ai.model.exception.AuthenticationException;
import com.sertac.ai.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveRefreshToken() {
        String tokenId = "tokenId";
        String email = "test@example.com";
        String token = "refreshToken";
        Date expiryDate = new Date();

        refreshTokenService.saveRefreshToken(tokenId, email, token, expiryDate);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findByToken() {
        String token = "refreshToken";
        RefreshToken refreshToken = new RefreshToken();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken(token);

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());
    }

    @Test
    void deactivateRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshTokenService.deactivateRefreshToken(refreshToken);

        assertEquals(RefreshTokenStatus.INACTIVE, refreshToken.getStatus());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeRefreshToken() {
        String token = "refreshToken";
        RefreshToken refreshToken = new RefreshToken();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeRefreshToken(token);

        assertEquals(RefreshTokenStatus.INACTIVE, refreshToken.getStatus());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void revokeRefreshToken_tokenNotFound() {
        String token = "nonExistentToken";
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> refreshTokenService.revokeRefreshToken(token));
    }

    @Test
    void blacklistToken() {
        String token = "refreshToken";
        RefreshToken refreshToken = new RefreshToken();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.blacklistToken(token);

        assertEquals(RefreshTokenStatus.BLACKLISTED, refreshToken.getStatus());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void blacklistToken_tokenNotFound() {
        String token = "nonExistentToken";
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> refreshTokenService.blacklistToken(token));
    }

    @Test
    void isTokenBlacklisted() {
        String token = "refreshToken";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setStatus(RefreshTokenStatus.BLACKLISTED);
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        boolean result = refreshTokenService.isTokenBlacklisted(token);

        assertTrue(result);
    }

    @Test
    void isTokenBlacklisted_tokenNotFound() {
        String token = "nonExistentToken";
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        boolean result = refreshTokenService.isTokenBlacklisted(token);

        assertFalse(result);
    }

    @Test
    void findActiveTokensByEmail() {
        String email = "test@example.com";
        List<RefreshToken> activeTokens = List.of(new RefreshToken(), new RefreshToken());
        when(refreshTokenRepository.findByEmailAndStatus(email, RefreshTokenStatus.ACTIVE)).thenReturn(activeTokens);

        List<RefreshToken> result = refreshTokenService.findActiveTokensByEmail(email);

        assertEquals(activeTokens, result);
    }
}