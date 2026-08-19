package com.omnibid.identity.dto;

import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserProfile;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String phoneNumber,
        String locale,
        String timezone,
        String bio,
        String status,
        Set<String> roles
) {
    public static UserResponse from(UserAccount user, UserProfile profile) {
        return new UserResponse(
                user.getId(),
                user.getPrimaryEmail(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getPhoneNumber(),
                profile.getLocale(),
                profile.getTimezone(),
                profile.getBio(),
                user.getStatus().name(),
                user.getRoles().stream()
                        .map(Role::getCode)
                        .map(Enum::name)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
