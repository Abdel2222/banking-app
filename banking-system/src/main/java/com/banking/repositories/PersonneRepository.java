package com.banking.repositories;

import com.banking.entities.Personne;
import com.banking.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonneRepository extends JpaRepository<Personne, Long> {

    // ✅ Utilisation propre de l'enum
    long countByRole(Role role);

    // Optionnel : méthode pour trouver une personne par son rôle
    List<Personne> findByRole(Role role);

    // Pour login, etc.
    Optional<Personne> findByEmail(String email);
}
