package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                select token
                from PasswordResetToken token
                join fetch token.user
                where token.tokenHash = :tokenHash
                """)
        Optional<PasswordResetToken> findForUpdateByTokenHash(
                @Param("tokenHash") String tokenHash
        );

    @Modifying
    @Query("""
            update PasswordResetToken token
            set token.usedAt = :now
            where token.user.id = :userId and token.usedAt is null
            """)
    int invalidateActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
