package com.omnibid.wallet.config;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class DemoDataConfig {

    public static final UUID DEMO_BIDDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID DEMO_BIDDER_2_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Bean
    CommandLineRunner seedWallet(WalletRepository repository) {
        return args -> {
            seedWallet(repository, DEMO_BIDDER_ID);
            seedWallet(repository, DEMO_BIDDER_2_ID);
        };
    }

    private void seedWallet(WalletRepository repository, UUID userId) {
        if (repository.existsById(userId)) {
            return;
        }

        Wallet wallet = new Wallet();
        wallet.setId(userId);
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("1000000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO.setScale(2));
        repository.save(wallet);
    }
}
