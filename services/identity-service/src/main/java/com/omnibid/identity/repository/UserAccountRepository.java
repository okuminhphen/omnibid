package com.omnibid.identity.repository;

import com.omnibid.identity.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByPrimaryEmailIgnoreCase(String primaryEmail);
}
