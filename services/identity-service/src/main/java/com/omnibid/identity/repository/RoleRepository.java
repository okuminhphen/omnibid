package com.omnibid.identity.repository;

import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByCode(RoleCode code);
}
