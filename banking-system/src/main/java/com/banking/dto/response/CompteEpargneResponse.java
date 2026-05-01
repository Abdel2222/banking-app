package com.banking.dto.response;

import com.banking.entities.CompteEpargne;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CompteEpargneResponse {
    private Long id;
    private String numCompteBancaire;
    private String numCompteEpargne;
    private BigDecimal soldeEpargne;
    private BigDecimal tauxInteret;
    private BigDecimal taxationVirtuelle;   // colonne virtuelle
    private LocalDateTime derniereCapitalisation;

    /** Fabrique un DTO à partir de l'entité (récupère le numCompteBancaire via la relation) */
    public static CompteEpargneResponse fromEntity(CompteEpargne epargne) {
        if (epargne == null) return null;
        String numCB = (epargne.getCompteBancaire() != null)
                ? epargne.getCompteBancaire().getNumCompte()
                : null;

        return CompteEpargneResponse.builder()
                .id(epargne.getId())
                .numCompteBancaire(numCB)
                .numCompteEpargne(epargne.getNumCompteEpargne())
                .soldeEpargne(epargne.getSoldeEpargne())
                .tauxInteret(epargne.getTauxInteret())
                .taxationVirtuelle(epargne.getTaxationVirtuelle())
                .derniereCapitalisation(epargne.getDateDerniereCapitalisation())
                .build();
    }

    /** Variante avec fallback si tu as déjà le numCompteBancaire en paramètre */
    public static CompteEpargneResponse fromEntity(CompteEpargne epargne, String fallbackNumCompteBancaire) {
        if (epargne == null) return null;
        String numCB = (epargne.getCompteBancaire() != null && epargne.getCompteBancaire().getNumCompte() != null)
                ? epargne.getCompteBancaire().getNumCompte()
                : fallbackNumCompteBancaire;

        return CompteEpargneResponse.builder()
                .id(epargne.getId())
                .numCompteBancaire(numCB)
                .numCompteEpargne(epargne.getNumCompteEpargne())
                .soldeEpargne(epargne.getSoldeEpargne())
                .tauxInteret(epargne.getTauxInteret())
                .taxationVirtuelle(epargne.getTaxationVirtuelle())
                .derniereCapitalisation(epargne.getDateDerniereCapitalisation())
                .build();
    }
}
