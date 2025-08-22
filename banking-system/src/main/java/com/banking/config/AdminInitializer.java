package com.banking.config;

import com.banking.entities.Client;
import com.banking.entity.enums.Role;
import com.banking.repositories.PersonneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    @Profile("!prod") // ⚠️ évite de créer ces comptes en prod
    CommandLineRunner initAdmins(PersonneRepository personneRepository,
                                 PasswordEncoder passwordEncoder) {
        return args -> {

            // === SUPER_ADMIN ===
            String superEmail = "root@bank.local";
            if (personneRepository.findByEmail(superEmail).isEmpty()) {
                Client superAdmin = new Client(
                        "Root",
                        "SuperAdmin",
                        superEmail,
                        passwordEncoder.encode("Root#2025!")
                );
                superAdmin.setRole(Role.SUPER_ADMIN); // ✅ rôle enum
                personneRepository.save(superAdmin);
                System.out.println("✅ SUPER_ADMIN créé : " + superEmail);
            } else {
                System.out.println("ℹ SUPER_ADMIN déjà existant : " + superEmail);
            }

            // === ADMIN ===
            String adminEmail = "admin@bank.local";
            if (personneRepository.findByEmail(adminEmail).isEmpty()) {
                Client admin = new Client(
                        "John",
                        "Admin",
                        adminEmail,
                        passwordEncoder.encode("Admin#2025!")
                );
                admin.setRole(Role.ADMIN); // ✅ rôle enum
                personneRepository.save(admin);
                System.out.println("✅ ADMIN créé : " + adminEmail);
            } else {
                System.out.println("ℹ ADMIN déjà existant : " + adminEmail);
            }
        };
    }
}
