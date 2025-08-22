package com.banking.dto.response;

import com.banking.entities.CompteEpargne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteEpargneResponse {

    private Long id;
    private String numCompte;
    private BigDecimal soldeEpargne;
    private BigDecimal tauxInteret;
    private LocalDateTime dateDerniereCapitalisation;

    /** Surcharge sûre (on fournit le numCompte nous-mêmes depuis le contrôleur) */
    public static CompteEpargneResponse fromEntity(CompteEpargne epargne, String numCompte) {
        if (epargne == null) return null;
        return CompteEpargneResponse.builder()
                .id(epargne.getId())
                .numCompte(numCompte)
                .soldeEpargne(epargne.getSoldeEpargne())
                .tauxInteret(epargne.getTauxInteret())
                .dateDerniereCapitalisation(epargne.getDateDerniereCapitalisation())
                .build();
    }

    /** Compat : si tu ne passes pas le numCompte, on essaie de le déduire (peut être null si LAZY) */
    public static CompteEpargneResponse fromEntity(CompteEpargne epargne) {
        if (epargne == null) return null;
        String maybeNum =
                (epargne.getCompteBancaire() != null ? epargne.getCompteBancaire().getNumCompte() : null);
        return fromEntity(epargne, maybeNum);
    }
}

