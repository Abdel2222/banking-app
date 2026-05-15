package com.banking.controllers;

import com.banking.entities.Personne;
import com.banking.entity.enums.Role;
import com.banking.repositories.PersonneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
public class EmployeController {

    private final PersonneRepository personneRepository;
    private final Random random = new Random();

    @GetMapping
    public List<Personne> getAllEmployes() {
        return personneRepository.findByRole(Role.EMPLOYE);
    }

    @GetMapping("/random")
    public Personne getRandomEmploye() {
        List<Personne> employes = personneRepository.findByRole(Role.EMPLOYE);

        if (employes.isEmpty()) {
            throw new RuntimeException("Aucun employé disponible");
        }

        return employes.get(random.nextInt(employes.size()));
    }

    @GetMapping("/random/depot")
    public Personne getRandomEmployeDepot() {
        List<Personne> employes = personneRepository.findByRole(Role.EMPLOYE)
                .stream()
                .filter(e -> e.getPoste() != null && e.getPoste().toLowerCase().contains("dépôt"))
                .toList();

        if (employes.isEmpty()) {
            return getRandomEmploye();
        }

        return employes.get(random.nextInt(employes.size()));
    }

    @GetMapping("/random/retrait")
    public Personne getRandomEmployeRetrait() {
        List<Personne> employes = personneRepository.findByRole(Role.EMPLOYE)
                .stream()
                .filter(e -> e.getPoste() != null && e.getPoste().toLowerCase().contains("retrait"))
                .toList();

        if (employes.isEmpty()) {
            return getRandomEmploye();
        }

        return employes.get(random.nextInt(employes.size()));
    }

    @GetMapping("/count")
    public Map<String, Object> countEmployes() {
        return Map.of(
                "role", "EMPLOYE",
                "total", personneRepository.countByRole(Role.EMPLOYE)
        );
    }
}