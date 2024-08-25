package com.sertac.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.sertac.ai.model.entity.RefreshToken;
import com.sertac.ai.model.enums.RefreshTokenStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByEmail(String email);
    List<RefreshToken> findByEmailAndStatus(String email, RefreshTokenStatus active);

}
