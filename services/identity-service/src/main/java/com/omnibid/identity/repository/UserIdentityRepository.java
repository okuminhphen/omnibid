package com.omnibid.identity.repository;

import com.omnibid.identity.domain.IdentityProvider;
import com.omnibid.identity.domain.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByProviderAndProviderSubject(
            IdentityProvider provider,
            String providerSubject
    );
}
