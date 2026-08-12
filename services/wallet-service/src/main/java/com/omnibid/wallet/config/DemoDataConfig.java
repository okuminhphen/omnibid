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

    @Bean
    CommandLineRunner seedWallet(WalletRepository repository) {
        return args -> {
            if (repository.existsById(DEMO_BIDDER_ID)) {
                return;
            }
            Wallet wallet = new Wallet();
            wallet.setId(DEMO_BIDDER_ID);
            wallet.setAvailableBalance(new BigDecimal("1000000.00"));
            wallet.setFrozenBalance(BigDecimal.ZERO.setScale(2));
            repository.save(wallet);
        };
    }
}
