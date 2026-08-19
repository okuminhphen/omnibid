package com.omnibid.identity.service;

import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserProfile;

public record AuthenticatedUser(UserAccount account, UserProfile profile) {
}
