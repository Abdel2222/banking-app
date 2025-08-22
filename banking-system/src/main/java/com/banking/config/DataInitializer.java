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
public class DataInitializer {

    @Bean
    @Profile("!prod")
    CommandLineRunner seedAdmins(PersonneRepository personneRepository,
                                 PasswordEncoder passwordEncoder) {
        return args -> {
            // SUPER_ADMIN
            String superEmail = "root@bank.local";
            if (personneRepository.findByEmail(superEmail).isEmpty()) {
                Client sa = new Client("Root", "SuperAdmin", superEmail,
                        passwordEncoder.encode("Root#2025!"));
                sa.setRole(Role.SUPER_ADMIN);

                personneRepository.save(sa);
                System.out.println("✅ SUPER_ADMIN créé : " + superEmail);
            } else {
                System.out.println("ℹ SUPER_ADMIN déjà existant : " + superEmail);
            }

            // ADMIN
            String adminEmail = "admin@bank.local";
            if (personneRepository.findByEmail(adminEmail).isEmpty()) {
                Client admin = new Client("John", "Admin", adminEmail,
                        passwordEncoder.encode("Admin#2025!"));
                admin.setRole(Role.ADMIN);  // Utilisation correcte de l'enum

                personneRepository.save(admin);
                System.out.println("✅ ADMIN créé : " + adminEmail);
            } else {
                System.out.println("ℹ ADMIN déjà existant : " + adminEmail);
            }
        };
    }
}

