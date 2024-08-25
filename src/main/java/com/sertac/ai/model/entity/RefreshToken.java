package com.sertac.ai.model.entity;

import java.util.Date;

import com.sertac.ai.model.enums.RefreshTokenStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RefreshTokenStatus status;

    public RefreshToken() {
    }

    public RefreshToken(String tokenId, String email, String token, Date expiryDate) {
        this.tokenId = tokenId;
        this.email = email;
        this.token = token;
        this.expiryDate = expiryDate;
        this.status = RefreshTokenStatus.ACTIVE;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public RefreshTokenStatus getStatus() {
        return status;
    }

    public void setStatus(RefreshTokenStatus status) {
        this.status = status;
    }
}

