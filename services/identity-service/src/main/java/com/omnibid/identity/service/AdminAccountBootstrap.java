package com.omnibid.identity.service;

import com.omnibid.identity.config.IdentityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountBootstrap implements ApplicationRunner {

    private final IdentityProperties properties;
    private final UserAccountService userAccountService;

    @Override
    public void run(ApplicationArguments args) {
        IdentityProperties.Admin admin = properties.admin();
        if (!admin.configured()) {
            log.warn("No bootstrap administrator is configured; set OMNIBID_ADMIN_EMAIL before deployment");
            return;
        }

        UUID adminId = userAccountService.provisionAdmin(admin.email(), admin.displayName());
        log.info("Bootstrap administrator account is ready: userId={}", adminId);
    }
}
