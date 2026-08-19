package com.omnibid.identity.repository;

import com.omnibid.identity.domain.IdentityOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityOutboxEventRepository extends JpaRepository<IdentityOutboxEvent, UUID> {
    List<IdentityOutboxEvent> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
