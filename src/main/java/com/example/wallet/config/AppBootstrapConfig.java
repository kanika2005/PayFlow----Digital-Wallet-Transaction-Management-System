package com.example.wallet.config;

import com.example.wallet.entity.Role;
import com.example.wallet.entity.RoleName;
import com.example.wallet.entity.User;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.RoleRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppBootstrapConfig {

    @Bean
    CommandLineRunner bootstrap(RoleRepository roleRepository,
                                UserRepository userRepository,
                                WalletRepository walletRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email}") String adminEmail,
                                @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            roleRepository.findByName(RoleName.USER).orElseGet(() -> roleRepository.save(new Role(RoleName.USER)));
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

            userRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(existingAdmin -> {
                if (walletRepository.findByUser_Id(existingAdmin.getId()).isEmpty()) {
                    Wallet wallet = new Wallet(existingAdmin);
                    wallet.setBalance(BigDecimal.ZERO);
                    walletRepository.save(wallet);
                    existingAdmin.setWallet(wallet);
                    userRepository.save(existingAdmin);
                }
            }, () -> {
                User admin = new User("System Admin", adminEmail.toLowerCase(), passwordEncoder.encode(adminPassword), adminRole);
                userRepository.save(admin);
                Wallet wallet = new Wallet(admin);
                wallet.setBalance(BigDecimal.ZERO);
                walletRepository.save(wallet);
                admin.setWallet(wallet);
                userRepository.save(admin);
            });
        };
    }
}