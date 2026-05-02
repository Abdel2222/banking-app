package com.banking.services.impl;

import com.banking.entities.Client;
import com.banking.entities.Personne;
import com.banking.entity.enums.Role;
import com.banking.repositories.PersonneRepository;
import com.banking.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final PersonneRepository personneRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.max:2}")
    private int maxAdmins;

    @Value("${app.security.superadmin.max:1}")
    private int maxSuperAdmins;

    public AdminServiceImpl(PersonneRepository personneRepo, PasswordEncoder passwordEncoder) {
        this.personneRepo = personneRepo;
        this.passwordEncoder = passwordEncoder;
    }

    private static void requireNonBlank(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Le champ '" + field + "' est obligatoire.");
        }
    }

    private void ensureEmailFree(String email) {
        if (personneRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email déjà utilisé: " + email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countAdmins() {
        return personneRepo.countByRole(Role.ADMIN);
    }

    @Override
    public Personne createAdmin(String prenom, String nom, String email, String rawPassword) {
        requireNonBlank(prenom, "prenom");
        requireNonBlank(nom, "nom");
        requireNonBlank(email, "email");
        requireNonBlank(rawPassword, "password");

        if (personneRepo.countByRole(Role.ADMIN) >= maxAdmins) {
            throw new IllegalStateException("Nombre maximum d'admins atteint (" + maxAdmins + ").");
        }
        ensureEmailFree(email);

        Client admin = new Client(prenom, nom, email, passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);

        return personneRepo.save(admin);
    }

    @Override
    public Personne createSuperAdmin(String prenom, String nom, String email, String rawPassword) {
        requireNonBlank(prenom, "prenom");
        requireNonBlank(nom, "nom");
        requireNonBlank(email, "email");
        requireNonBlank(rawPassword, "password");

        long countSa = personneRepo.countByRole(Role.SUPER_ADMIN);
        if (countSa >= maxSuperAdmins) {
            throw new IllegalStateException("Nombre maximum de super-admins atteint (" + maxSuperAdmins + ").");
        }
        ensureEmailFree(email);

        Client superAdmin = new Client(prenom, nom, email, passwordEncoder.encode(rawPassword));
        superAdmin.setRole(Role.SUPER_ADMIN);

        return personneRepo.save(superAdmin);
    }

    @Override
    public Personne promoteToAdmin(Long userId) {
        Personne p = personneRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable: " + userId));

        if (p.getRole() == Role.ADMIN) {
            return p;
        }

        if (personneRepo.countByRole(Role.ADMIN) >= maxAdmins) {
            throw new IllegalStateException("Nombre maximum d'admins atteint (" + maxAdmins + ").");
        }

        p.setRole(Role.ADMIN);
        return personneRepo.save(p);
    }

    @Override
    public Personne demoteToClient(Long userId) {
        Personne p = personneRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable: " + userId));
        p.setRole(Role.CLIENT);
        return personneRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personne> listAdmins() {
        return personneRepo.findAll().stream()
                .filter(u -> {
                    Role r = u.getRole();
                    return r == Role.ADMIN || r == Role.SUPER_ADMIN;
                })
                .toList();
    }
}
