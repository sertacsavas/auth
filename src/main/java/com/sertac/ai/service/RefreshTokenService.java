package com.sertac.ai.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sertac.ai.model.entity.RefreshToken;
import com.sertac.ai.model.enums.RefreshTokenStatus;
import com.sertac.ai.model.exception.AuthenticationException;
import com.sertac.ai.repository.RefreshTokenRepository;

@Service
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void saveRefreshToken(String tokenId, String email, String token, Date expiryDate) {
        RefreshToken refreshToken = new RefreshToken(tokenId,email, token, expiryDate);
        refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deactivateRefreshToken(RefreshToken refreshToken) {
        refreshToken.setStatus(RefreshTokenStatus.INACTIVE);
        refreshTokenRepository.save(refreshToken);
    }

    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("Refresh token not found"));
        deactivateRefreshToken(refreshToken);
    }

    public void blacklistToken(String token) {
        RefreshToken refreshToken = findByToken(token)
            .orElseThrow(() -> new AuthenticationException("Token not found"));
        refreshToken.setStatus(RefreshTokenStatus.BLACKLISTED);
        refreshTokenRepository.save(refreshToken);
    }

    public boolean isTokenBlacklisted(String token) {
        return findByToken(token)
            .map(t -> RefreshTokenStatus.BLACKLISTED.equals(t.getStatus()))
            .orElse(false);
    }

    public List<RefreshToken> findActiveTokensByEmail(String email) {
        return refreshTokenRepository.findByEmailAndStatus(email, RefreshTokenStatus.ACTIVE);
    }
}