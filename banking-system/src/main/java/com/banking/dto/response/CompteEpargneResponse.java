package com.banking.dto.response;

import com.banking.entities.CompteEpargne;
import com.banking.entity.enums.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CompteEpargneResponse {

    private Long          id;
    private String        numCompte;       // hérité de CompteBancaire
    private BigDecimal    premierMontant;  // seul attribut propre
    private BigDecimal    solde;           // balance héritée
    private AccountStatus statut;         // hérité
    private String        devise;         // hérité
    private Long          clientId;       // hérité
    private LocalDateTime createdAt;      // hérité

    /** Factory depuis l'entité */
    public static CompteEpargneResponse fromEntity(CompteEpargne ce) {
        if (ce == null) return null;
        return CompteEpargneResponse.builder()
                .id(ce.getId())
                .numCompte(ce.getNumCompte())
                .premierMontant(ce.getPremierMontant())
                .solde(ce.getBalance())
                .statut(ce.getStatus())
                .devise(ce.getDevise())
                .clientId(ce.getClient() != null ? ce.getClient().getId() : null)
                .createdAt(ce.getCreatedAt())
                .build();
    }
}