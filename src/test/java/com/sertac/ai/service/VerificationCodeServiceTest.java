package com.sertac.ai.service;

import com.sertac.ai.model.entity.VerificationCode;
import com.sertac.ai.model.enums.VerificationCodeStatus;
import com.sertac.ai.repository.VerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VerificationCodeServiceTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @InjectMocks
    private VerificationCodeService verificationCodeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateVerificationCode_shouldReturnSixDigitCode() {
        String code = verificationCodeService.generateVerificationCode();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void saveVerificationCode_shouldSaveCodeWithExpirationTime() {
        String email = "test@example.com";
        String code = "123456";
        
        VerificationCode verificationCode = new VerificationCode(email, code);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(verificationCode);
        
        verificationCodeService.saveVerificationCode(verificationCode);
        
        verify(verificationCodeRepository, times(1)).save(argThat(vc -> 
            vc.getEmail().equals(email) &&
            vc.getCode().equals(code) &&
            vc.getExpirationTime().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void verifyCode_shouldReturnTrueForValidCode() {
        String email = "test@example.com";
        String code = "123456";
        VerificationCode validCode = new VerificationCode(email, code);
        validCode.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        validCode.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(validCode));

        assertTrue(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void verifyCode_shouldReturnFalseForInvalidCode() {
        String email = "test@example.com";
        String validCode = "123456";
        String invalidCode = "654321";
        VerificationCode storedCode = new VerificationCode(email, validCode);
        storedCode.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        storedCode.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(storedCode));

        assertFalse(verificationCodeService.verifyCode(email, invalidCode));
    }

    @Test
    void verifyCode_shouldReturnFalseForExpiredCode() {
        String email = "test@example.com";
        String code = "123456";
        VerificationCode expiredCode = new VerificationCode(email, code);
        expiredCode.setExpirationTime(LocalDateTime.now().minusMinutes(1));
        expiredCode.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(expiredCode));

        assertFalse(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void deactivateVerificationCode_shouldChangeStatusToInactive() {
        // Arrange
        String email = "test@example.com";
        VerificationCode code1 = new VerificationCode();
        code1.setEmail(email);
        code1.setStatus(VerificationCodeStatus.ACTIVE);
        VerificationCode code2 = new VerificationCode();
        code2.setEmail(email);
        code2.setStatus(VerificationCodeStatus.ACTIVE);
        List<VerificationCode> activeCodes = Arrays.asList(code1, code2);

        when(verificationCodeRepository.findAllByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(activeCodes);

        // Act
        verificationCodeService.deactivateVerificationCode(email);

        // Assert
        verify(verificationCodeRepository).findAllByEmailAndStatus(email, VerificationCodeStatus.ACTIVE);
        verify(verificationCodeRepository).saveAll(activeCodes);

        for (VerificationCode code : activeCodes) {
            assertEquals(VerificationCodeStatus.INACTIVE, code.getStatus());
        }
    }

    @Test
    void testGenerateVerificationCode() {
        String code = verificationCodeService.generateVerificationCode();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testSaveVerificationCode_ValidEmail() {
        VerificationCode code = new VerificationCode();
        code.setEmail("test@example.com");
        code.setCode("123456");

        verificationCodeService.saveVerificationCode(code);

        verify(verificationCodeRepository).save(code);
        assertNotNull(code.getExpirationTime());
    }

    @Test
    void testSaveVerificationCode_InvalidEmail() {
        VerificationCode code = new VerificationCode();
        code.setEmail("invalid-email");
        code.setCode("123456");

        assertThrows(IllegalArgumentException.class, () -> verificationCodeService.saveVerificationCode(code));
    }

    @Test
    void testVerifyCode_ValidCode() {
        String email = "test@example.com";
        String code = "123456";
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setStatus(VerificationCodeStatus.ACTIVE);
        verificationCode.setExpirationTime(LocalDateTime.now().plusMinutes(5));

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(verificationCode));

        assertTrue(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void testVerifyCode_InvalidCode() {
        String email = "test@example.com";
        String code = "123456";
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode("654321");
        verificationCode.setStatus(VerificationCodeStatus.ACTIVE);
        verificationCode.setExpirationTime(LocalDateTime.now().plusMinutes(5));

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(verificationCode));

        assertFalse(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void testVerifyCode_ExpiredCode() {
        String email = "test@example.com";
        String code = "123456";
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setStatus(VerificationCodeStatus.ACTIVE);
        verificationCode.setExpirationTime(LocalDateTime.now().minusMinutes(1));

        when(verificationCodeRepository.findByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(Optional.of(verificationCode));

        assertFalse(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void testVerifyCode_MaxAttemptsReached() {
        String email = "test@example.com";
        String code = "123456";

        for (int i = 0; i < 5; i++) {
            verificationCodeService.verifyCode(email, "wrong-code");
        }

        assertFalse(verificationCodeService.verifyCode(email, code));
    }

    @Test
    void testUpdateExpiredCodes() {
        List<VerificationCode> expiredCodes = Arrays.asList(
            new VerificationCode(), new VerificationCode()
        );

        when(verificationCodeRepository.findAllByExpirationTimeBeforeAndStatus(any(LocalDateTime.class), eq(VerificationCodeStatus.ACTIVE)))
            .thenReturn(expiredCodes);

        verificationCodeService.updateExpiredCodes();

        verify(verificationCodeRepository).saveAll(expiredCodes);
        expiredCodes.forEach(code -> assertEquals(VerificationCodeStatus.EXPIRED, code.getStatus()));
    }

    @Test
    void testHasRecentActiveVerificationCode_True() {
        String email = "test@example.com";
        VerificationCode recentCode = new VerificationCode();
        recentCode.setEmail(email);
        recentCode.setCreatedAt(LocalDateTime.now().minusMinutes(4));
        recentCode.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.existsByEmailAndCreatedAtAfterAndStatus(
            eq(email), 
            any(LocalDateTime.class), 
            eq(VerificationCodeStatus.ACTIVE)))
            .thenReturn(true);

        assertTrue(verificationCodeService.hasRecentActiveVerificationCode(email));
    }

    @Test
    void testHasRecentActiveVerificationCode_False() {
        String email = "test@example.com";
        
        // Case 1: No recent code
        when(verificationCodeRepository.existsByEmailAndCreatedAtAfterAndStatus(
            eq(email), 
            any(LocalDateTime.class), 
            eq(VerificationCodeStatus.ACTIVE)))
            .thenReturn(false);
        
        assertFalse(verificationCodeService.hasRecentActiveVerificationCode(email));

        // Case 2: Code exists but is older than 5 minutes
        VerificationCode oldCode = new VerificationCode();
        oldCode.setEmail(email);
        oldCode.setCreatedAt(LocalDateTime.now().minusMinutes(6));
        oldCode.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.existsByEmailAndCreatedAtAfterAndStatus(
            eq(email), 
            any(LocalDateTime.class), 
            eq(VerificationCodeStatus.ACTIVE)))
            .thenReturn(false);

        assertFalse(verificationCodeService.hasRecentActiveVerificationCode(email));
    }
}