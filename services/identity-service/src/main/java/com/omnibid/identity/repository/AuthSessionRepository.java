package com.omnibid.identity.repository;

import com.omnibid.identity.domain.AuthSession;
import com.omnibid.identity.domain.AuthSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<AuthSession> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AuthSession session where session.refreshTokenHash = :refreshTokenHash")
    Optional<AuthSession> findForUpdateByRefreshTokenHash(String refreshTokenHash);

    List<AuthSession> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("""
            update AuthSession session
               set session.status = :status,
                   session.revokedAt = :revokedAt
             where session.tokenFamilyId = :familyId
               and session.status = com.omnibid.identity.domain.AuthSessionStatus.ACTIVE
            """)
    int revokeFamily(UUID familyId, AuthSessionStatus status, Instant revokedAt);

    @Modifying
    @Query("""
            update AuthSession session
               set session.status = :status,
                   session.revokedAt = :revokedAt
             where session.userId = :userId
               and session.status = com.omnibid.identity.domain.AuthSessionStatus.ACTIVE
            """)
    int revokeAllForUser(UUID userId, AuthSessionStatus status, Instant revokedAt);
}
