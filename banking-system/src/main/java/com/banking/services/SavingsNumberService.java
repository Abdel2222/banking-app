package com.banking.services;

import com.banking.repositories.CompteEpargneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class SavingsNumberService {

    private static final int LEN    = 16;
    private static final int PREFIX = 3;

    private final CompteEpargneRepository repo;
    private final SecureRandom rnd = new SecureRandom();

    public String fromBase(String baseNum) {
        String digits = baseNum == null ? "" : baseNum.replaceAll("\\D", "");
        String prefix = digits.substring(0, Math.min(PREFIX, digits.length()));
        int rest = Math.max(0, LEN - prefix.length());

        for (int i = 0; i < 50; i++) {
            String cand = prefix + randomDigits(rest);
            // ✅ existsByNumCompte au lieu de existsByNumCompteEpargne
            if (!repo.existsByNumCompte(cand)) return cand;
        }
        throw new IllegalStateException("Collisions répétées sur num_compte");
    }

    private String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(rnd.nextInt(10));
        return sb.toString();
    }
}
