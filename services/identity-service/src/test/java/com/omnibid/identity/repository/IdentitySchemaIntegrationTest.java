package com.omnibid.identity.repository;

import com.omnibid.identity.domain.IdentityProvider;
import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.RoleCode;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserIdentity;
import com.omnibid.identity.domain.UserProfile;
import com.omnibid.identity.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IdentitySchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("identity_test")
            .withUsername("omnibid")
            .withPassword("omnibid");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private UserAccountRepository userRepository;
    @Autowired private UserProfileRepository profileRepository;
    @Autowired private UserIdentityRepository identityRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySchemaPersistsGoogleIdentityAndRejectsRemovedLocalProvider() {
        UUID userId = UUID.randomUUID();
        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER).orElseThrow();

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPrimaryEmail("customer@omnibid.test");
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(customerRole);
        userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName("Customer");
        profile.setLocale("vi-VN");
        profile.setTimezone("Asia/Ho_Chi_Minh");
        profileRepository.saveAndFlush(profile);

        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(userId);
        identity.setProvider(IdentityProvider.GOOGLE);
        identity.setProviderSubject("google-subject");
        identity.setProviderEmail("customer@omnibid.test");
        identity.setEmailVerified(true);
        identity.setLastUsedAt(Instant.now());
        identityRepository.saveAndFlush(identity);

        assertThat(identityRepository.findByProviderAndProviderSubject(
                IdentityProvider.GOOGLE,
                "google-subject"
        )).isPresent();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO user_identities(
                    id, user_id, provider, provider_subject, email_verified, last_used_at
                ) VALUES (?, ?, 'LOCAL_DEV', ?, true, now())
                """,
                UUID.randomUUID(),
                userId,
                "removed-local-provider"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
