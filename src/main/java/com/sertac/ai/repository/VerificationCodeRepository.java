package com.sertac.ai.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.enums.VerificationCodeStatus;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmailAndStatus(String email, VerificationCodeStatus status);

    void deleteByCreatedAtBeforeAndStatus(LocalDateTime expirationTime, VerificationCodeStatus status);

    void deleteByExpirationTimeBefore(LocalDateTime expirationTime);

    void deleteByExpirationTimeBeforeAndStatus(LocalDateTime expirationTime, VerificationCodeStatus status);

    void deleteByEmailAndStatus(String email, VerificationCodeStatus status);

    List<VerificationCode> findAllByEmailAndStatus(String email, VerificationCodeStatus status);

    List<VerificationCode> findAllByEmailAndCreatedAtAfter(String email, LocalDateTime recentTime);

    List<VerificationCode> findAllByExpirationTimeBeforeAndStatus(LocalDateTime expirationTime,
            VerificationCodeStatus active);
}