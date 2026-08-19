package com.omnibid.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.identity.domain.IdentityOutboxEvent;
import com.omnibid.identity.domain.IdentityProvider;
import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.RoleCode;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserIdentity;
import com.omnibid.identity.domain.UserProfile;
import com.omnibid.identity.domain.UserStatus;
import com.omnibid.identity.dto.UpdateProfileRequest;
import com.omnibid.identity.exception.AccountLinkRequiredException;
import com.omnibid.identity.exception.AuthenticationException;
import com.omnibid.identity.repository.IdentityOutboxEventRepository;
import com.omnibid.identity.repository.RoleRepository;
import com.omnibid.identity.repository.UserAccountRepository;
import com.omnibid.identity.repository.UserIdentityRepository;
import com.omnibid.identity.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    public static final UUID CUSTOMER_A_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID CUSTOMER_B_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private final UserAccountRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final IdentityOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthenticatedUser findOrCreateGoogle(GoogleCredentialService.GooglePrincipal principal) {
        UserIdentity existingIdentity = identityRepository
                .findByProviderAndProviderSubject(IdentityProvider.GOOGLE, principal.subject())
                .orElse(null);
        if (existingIdentity != null) {
            existingIdentity.setLastUsedAt(Instant.now());
            existingIdentity.setProviderEmail(principal.email());
            UserAccount user = requireActiveUser(existingIdentity.getUserId());
            user.setLastLoginAt(Instant.now());
            return new AuthenticatedUser(user, requireProfile(user.getId()));
        }

        if (userRepository.findByPrimaryEmailIgnoreCase(principal.email()).isPresent()) {
            throw new AccountLinkRequiredException(
                    "An account already uses this email. Sign in to that account before linking Google."
            );
        }

        UUID userId = UUID.randomUUID();
        return createUser(
                userId,
                principal.email(),
                principal.displayName() == null || principal.displayName().isBlank()
                        ? principal.email()
                        : principal.displayName(),
                principal.avatarUrl(),
                IdentityProvider.GOOGLE,
                principal.subject(),
                principal.emailVerified(),
                RoleCode.CUSTOMER
        );
    }

    @Transactional
    public AuthenticatedUser findOrCreateDevUser(String alias) {
        DevIdentity dev = switch (alias.toLowerCase()) {
            case "customer-a" -> new DevIdentity(
                    CUSTOMER_A_ID,
                    "customer-a@omnibid.local",
                    "Người dùng A",
                    RoleCode.CUSTOMER
            );
            case "customer-b" -> new DevIdentity(
                    CUSTOMER_B_ID,
                    "customer-b@omnibid.local",
                    "Người dùng B",
                    RoleCode.CUSTOMER
            );
            case "admin" -> new DevIdentity(
                    ADMIN_ID,
                    "admin@omnibid.local",
                    "OmniBid Admin",
                    RoleCode.ADMIN
            );
            default -> throw new IllegalArgumentException("Unknown local user alias: " + alias);
        };

        UserIdentity identity = identityRepository
                .findByProviderAndProviderSubject(IdentityProvider.LOCAL_DEV, alias.toLowerCase())
                .orElse(null);
        if (identity != null) {
            identity.setLastUsedAt(Instant.now());
            UserAccount user = requireActiveUser(identity.getUserId());
            user.setLastLoginAt(Instant.now());
            return new AuthenticatedUser(user, requireProfile(user.getId()));
        }

        return createUser(
                dev.id(),
                dev.email(),
                dev.displayName(),
                null,
                IdentityProvider.LOCAL_DEV,
                alias.toLowerCase(),
                true,
                dev.role()
        );
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser get(UUID userId) {
        return new AuthenticatedUser(requireActiveUser(userId), requireProfile(userId));
    }

    @Transactional
    public AuthenticatedUser updateProfile(UUID userId, UpdateProfileRequest request) {
        UserAccount user = requireActiveUser(userId);
        UserProfile profile = requireProfile(userId);
        profile.setDisplayName(request.displayName().trim());
        profile.setPhoneNumber(normalizeNullable(request.phoneNumber()));
        profile.setLocale(request.locale().trim());
        profile.setTimezone(request.timezone().trim());
        profile.setBio(normalizeNullable(request.bio()));
        return new AuthenticatedUser(user, profile);
    }

    private AuthenticatedUser createUser(
            UUID userId,
            String email,
            String displayName,
            String avatarUrl,
            IdentityProvider provider,
            String providerSubject,
            boolean emailVerified,
            RoleCode roleCode
    ) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role is not seeded: " + roleCode));

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPrimaryEmail(email.toLowerCase());
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(Instant.now());
        user.getRoles().add(role);
        userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(displayName);
        profile.setAvatarUrl(avatarUrl);
        profile.setLocale("vi-VN");
        profile.setTimezone("Asia/Ho_Chi_Minh");
        profileRepository.save(profile);

        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(userId);
        identity.setProvider(provider);
        identity.setProviderSubject(providerSubject);
        identity.setProviderEmail(email.toLowerCase());
        identity.setEmailVerified(emailVerified);
        identity.setLastUsedAt(Instant.now());
        identityRepository.save(identity);

        createUserRegisteredOutbox(userId);
        return new AuthenticatedUser(user, profile);
    }

    private void createUserRegisteredOutbox(UUID userId) {
        UUID eventId = UUID.randomUUID();
        try {
            IdentityOutboxEvent event = new IdentityOutboxEvent();
            event.setId(eventId);
            event.setAggregateType("UserAccount");
            event.setAggregateId(userId);
            event.setEventType("UserRegistered");
            event.setPayload(objectMapper.writeValueAsString(Map.of(
                    "eventId", eventId,
                    "eventType", "UserRegistered",
                    "schemaVersion", 1,
                    "userId", userId,
                    "occurredAt", Instant.now()
            )));
            event.setCreatedAt(Instant.now());
            outboxRepository.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize UserRegistered event", exception);
        }
    }

    private UserAccount requireActiveUser(UUID userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        if (!user.isActive()) {
            throw new AuthenticationException("User account is not active");
        }
        return user;
    }

    private UserProfile requireProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User profile is missing: " + userId));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DevIdentity(UUID id, String email, String displayName, RoleCode role) {
    }
}
