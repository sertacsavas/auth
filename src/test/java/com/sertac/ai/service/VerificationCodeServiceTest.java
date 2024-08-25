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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    void deactivateVerificationCode_shouldChangeStatusToUsed() {
        String email = "test@example.com";
        VerificationCode activeCode1 = new VerificationCode(email, "123456");
        VerificationCode activeCode2 = new VerificationCode(email, "654321");
        activeCode1.setStatus(VerificationCodeStatus.ACTIVE);
        activeCode2.setStatus(VerificationCodeStatus.ACTIVE);

        when(verificationCodeRepository.findAllByEmailAndStatus(email, VerificationCodeStatus.ACTIVE))
            .thenReturn(List.of(activeCode1, activeCode2));

        verificationCodeService.deactivateVerificationCode(email);

        verify(verificationCodeRepository).saveAll(argThat(codes -> {
            List<VerificationCode> codeList = (List<VerificationCode>) codes;
            return codeList.size() == 2 &&
                   codeList.stream().allMatch(vc -> vc.getStatus() == VerificationCodeStatus.USED);
        }));
    }
}