package com.omnibid.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.domain.IdentityProvider;
import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.RoleCode;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserIdentity;
import com.omnibid.identity.domain.UserProfile;
import com.omnibid.identity.domain.UserStatus;
import com.omnibid.identity.repository.IdentityOutboxEventRepository;
import com.omnibid.identity.repository.RoleRepository;
import com.omnibid.identity.repository.UserAccountRepository;
import com.omnibid.identity.repository.UserIdentityRepository;
import com.omnibid.identity.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock private UserAccountRepository userRepository;
    @Mock private UserIdentityRepository identityRepository;
    @Mock private UserProfileRepository profileRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private IdentityOutboxEventRepository outboxRepository;

    private UserAccountService service;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        customerRole = role((short) 1, RoleCode.CUSTOMER);
        adminRole = role((short) 2, RoleCode.ADMIN);
        service = new UserAccountService(
                userRepository,
                identityRepository,
                profileRepository,
                roleRepository,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules(),
                properties("admin@omnibid.test")
        );
    }

    @Test
    void provisionsAdministratorWithoutPasswordOrProviderIdentity() {
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByPrimaryEmailIgnoreCase("admin@omnibid.test"))
                .thenReturn(Optional.empty());

        UUID userId = service.provisionAdmin(" ADMIN@omnibid.test ", "Primary Administrator");

        ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).saveAndFlush(account.capture());
        assertThat(account.getValue().getId()).isEqualTo(userId);
        assertThat(account.getValue().getPrimaryEmail()).isEqualTo("admin@omnibid.test");
        assertThat(account.getValue().getRoles()).containsExactly(adminRole);
        verify(identityRepository, never()).save(any());
        verify(outboxRepository).save(any());
    }

    @Test
    void firstGoogleLoginCreatesCustomerByDefault() {
        GoogleCredentialService.GooglePrincipal principal = principal("customer@omnibid.test");
        when(identityRepository.findByProviderAndProviderSubject(IdentityProvider.GOOGLE, principal.subject()))
                .thenReturn(Optional.empty());
        when(userRepository.findByPrimaryEmailIgnoreCase(principal.email())).thenReturn(Optional.empty());
        when(roleRepository.findByCode(RoleCode.CUSTOMER)).thenReturn(Optional.of(customerRole));

        AuthenticatedUser result = service.findOrCreateGoogle(principal);

        assertThat(result.account().getRoles()).containsExactly(customerRole);
        assertThat(result.account().getPrimaryEmail()).isEqualTo(principal.email());
        verify(identityRepository).save(any(UserIdentity.class));
    }

    @Test
    void verifiedGoogleEmailCanClaimOnlyPreProvisionedAdministrator() {
        GoogleCredentialService.GooglePrincipal principal = principal("admin@omnibid.test");
        UserAccount admin = new UserAccount();
        admin.setId(UUID.randomUUID());
        admin.setPrimaryEmail(principal.email());
        admin.setStatus(UserStatus.ACTIVE);
        admin.getRoles().add(adminRole);
        UserProfile profile = new UserProfile();
        profile.setUserId(admin.getId());
        profile.setDisplayName("Primary Administrator");

        when(identityRepository.findByProviderAndProviderSubject(IdentityProvider.GOOGLE, principal.subject()))
                .thenReturn(Optional.empty());
        when(userRepository.findByPrimaryEmailIgnoreCase(principal.email())).thenReturn(Optional.of(admin));
        when(identityRepository.existsByUserId(admin.getId())).thenReturn(false);
        when(profileRepository.findById(admin.getId())).thenReturn(Optional.of(profile));

        AuthenticatedUser result = service.findOrCreateGoogle(principal);

        assertThat(result.account().getId()).isEqualTo(admin.getId());
        ArgumentCaptor<UserIdentity> identity = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(identity.capture());
        assertThat(identity.getValue().getUserId()).isEqualTo(admin.getId());
        assertThat(identity.getValue().getProvider()).isEqualTo(IdentityProvider.GOOGLE);
        verify(userRepository, never()).saveAndFlush(any());
    }

    private GoogleCredentialService.GooglePrincipal principal(String email) {
        return new GoogleCredentialService.GooglePrincipal(
                "google-subject-123",
                email,
                "Display Name",
                null,
                true
        );
    }

    private Role role(short id, RoleCode code) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        return role;
    }

    private IdentityProperties properties(String adminEmail) {
        return new IdentityProperties(
                "http://identity.test",
                "omnibid-api",
                Duration.ofMinutes(10),
                Duration.ofDays(30),
                "omnibid_refresh",
                false,
                "Lax",
                List.of("http://localhost:3000"),
                new IdentityProperties.Google(true, "client-id", "https://example.test/jwks"),
                new IdentityProperties.Admin(adminEmail, "Primary Administrator"),
                new IdentityProperties.Kafka("identity-events")
        );
    }
}
