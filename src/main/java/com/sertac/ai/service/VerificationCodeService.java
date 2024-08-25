package com.sertac.ai.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sertac.ai.email.EmailUtils;
import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.enums.VerificationCodeStatus;
import com.sertac.ai.repository.VerificationCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;

    // Constructor injection
    public VerificationCodeService(VerificationCodeRepository verificationCodeRepository) {
        this.verificationCodeRepository = verificationCodeRepository;
    }

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int CODE_EXPIRATION_MINUTES = 5;

    private ConcurrentHashMap<String, Integer> attemptCounter = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LocalDateTime> lockoutTime = new ConcurrentHashMap<>();

    public String generateVerificationCode() {
        SecureRandom secureRandom = new SecureRandom();
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    public void saveVerificationCode(VerificationCode verificationCode) {
        // Check if email is valid
        if (!EmailUtils.isValidEmail(verificationCode.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Deactivate any existing active codes for this email
        deactivateVerificationCode(verificationCode.getEmail());

        // Set expiration time and save the new code
        verificationCode.setExpirationTime(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES));
        verificationCodeRepository.save(verificationCode);
    }



    public boolean verifyCode(String email, String code) {
        if (isLocked(email)) {
            return false;
        }

        Optional<VerificationCode> verificationCodeOpt = verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE);
        if (verificationCodeOpt.isEmpty() || verificationCodeOpt.get().getExpirationTime().isBefore(LocalDateTime.now())) {
            incrementAttempt(email);
            return false;
        }

        VerificationCode verificationCode = verificationCodeOpt.get();
        boolean isValid = verificationCode.getCode().equals(code);
        if (isValid) {
            verificationCode.setStatus(VerificationCodeStatus.USED);
            verificationCodeRepository.save(verificationCode);
            resetAttempts(email);
        } else {
            incrementAttempt(email);
        }

        return isValid;
    }

    private boolean isLocked(String email) {
        LocalDateTime lockoutEndTime = lockoutTime.get(email);
        return lockoutEndTime != null && lockoutEndTime.isAfter(LocalDateTime.now());
    }

    private void incrementAttempt(String email) {
        int attempts = attemptCounter.getOrDefault(email, 0) + 1;
        attemptCounter.put(email, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            lockoutTime.put(email, LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        }
    }

    private void resetAttempts(String email) {
        attemptCounter.remove(email);
        lockoutTime.remove(email);
    }

    public void updateExpiredCodes() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(CODE_EXPIRATION_MINUTES);
        List<VerificationCode> expiredCodes = verificationCodeRepository.findAllByExpirationTimeBeforeAndStatus(expirationTime, VerificationCodeStatus.ACTIVE);
        expiredCodes.forEach(code -> code.setStatus(VerificationCodeStatus.EXPIRED));
        verificationCodeRepository.saveAll(expiredCodes);
    }


    public void deactivateVerificationCode(String email) {
        List<VerificationCode> activeCodes = verificationCodeRepository.findAllByEmailAndStatus(email, VerificationCodeStatus.ACTIVE);
        activeCodes.forEach(code -> code.setStatus(VerificationCodeStatus.INACTIVE));
        verificationCodeRepository.saveAll(activeCodes);
    }


    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredCodes() {
        // Update expired verification codes
        updateExpiredCodes();

        // Clear expired lockouts
        LocalDateTime now = LocalDateTime.now();
        lockoutTime.entrySet().removeIf(entry -> entry.getValue().isBefore(now));

        // Reset attempt counters for users who are not locked out
        attemptCounter.entrySet().removeIf(entry -> !lockoutTime.containsKey(entry.getKey()));
    }


    public boolean hasRecentActiveVerificationCode(String email) {
        // Define the time threshold (e.g., 5 minutes ago)
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        // Check for recent active verification codes
        return verificationCodeRepository.existsByEmailAndCreatedAtAfterAndStatus(
            email, fiveMinutesAgo, VerificationCodeStatus.ACTIVE);
    }

}