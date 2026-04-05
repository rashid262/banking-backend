package com.bank.transaction.config;

import com.bank.transaction.domain.User;
import com.bank.transaction.enums.Role;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.UserRepository;
import com.bank.transaction.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            PasswordEncoder encoder) {

        return (args) -> {
            // ONLY INITIALIZE THE SYSTEM ADMIN
            if (!accountRepository.existsById("UB-0000")) {

                // 1. Create foundation (Added email parameter to match AccountService)
                accountService.createNewUserAccount(
                        "UB-0000",
                        "admin",
                        "admin@universalbank.com",
                        "INR"
                );

                // 2. Fetch and elevate to Admin
                User adminUser = userRepository.findByUsername("admin")
                        .orElseThrow(() -> new RuntimeException("Admin user creation failed"));

                adminUser.setPasswordHash(encoder.encode("admin123"));
                adminUser.setRole(Role.ADMIN);
                adminUser.setVerified(true); // 👈 Important: Admin must be auto-verified
                adminUser.setVerificationToken(null);

                userRepository.save(adminUser);

                System.out.println("🏛️ Universal Bank Vault Initialized (UB-0000)");
                System.out.println("🔑 Admin credentials: admin / admin123");
            }
        };
    }
}