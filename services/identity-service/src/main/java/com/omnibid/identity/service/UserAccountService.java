package com.omnibid.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.identity.config.IdentityProperties;
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

    private final UserAccountRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final IdentityOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final IdentityProperties properties;

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

        UserAccount accountWithEmail = userRepository.findByPrimaryEmailIgnoreCase(principal.email()).orElse(null);
        if (accountWithEmail != null) {
            if (canClaimBootstrapAdmin(accountWithEmail, principal)) {
                attachGoogleIdentity(accountWithEmail.getId(), principal);
                accountWithEmail.setLastLoginAt(Instant.now());
                return new AuthenticatedUser(accountWithEmail, requireProfile(accountWithEmail.getId()));
            }
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
                isBootstrapAdminEmail(principal.email()) ? RoleCode.ADMIN : RoleCode.CUSTOMER
        );
    }

    @Transactional
    public UUID provisionAdmin(String email, String displayName) {
        String normalizedEmail = email.trim().toLowerCase();
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is not seeded"));

        UserAccount existing = userRepository.findByPrimaryEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existing != null) {
            existing.getRoles().add(adminRole);
            return existing.getId();
        }

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPrimaryEmail(normalizedEmail);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(adminRole);
        userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(displayName.trim());
        profile.setLocale("vi-VN");
        profile.setTimezone("Asia/Ho_Chi_Minh");
        profileRepository.save(profile);

        createUserRegisteredOutbox(userId);
        return userId;
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

    private void attachGoogleIdentity(
            UUID userId,
            GoogleCredentialService.GooglePrincipal principal
    ) {
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(userId);
        identity.setProvider(IdentityProvider.GOOGLE);
        identity.setProviderSubject(principal.subject());
        identity.setProviderEmail(principal.email().toLowerCase());
        identity.setEmailVerified(principal.emailVerified());
        identity.setLastUsedAt(Instant.now());
        identityRepository.save(identity);
    }

    private boolean canClaimBootstrapAdmin(
            UserAccount user,
            GoogleCredentialService.GooglePrincipal principal
    ) {
        return principal.emailVerified()
                && isBootstrapAdminEmail(principal.email())
                && user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.ADMIN)
                && !identityRepository.existsByUserId(user.getId());
    }

    private boolean isBootstrapAdminEmail(String email) {
        return properties.admin().configured()
                && properties.admin().email().equalsIgnoreCase(email);
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

}
