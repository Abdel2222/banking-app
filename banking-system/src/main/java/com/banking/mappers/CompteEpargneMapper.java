package com.banking.mappers;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;
import org.springframework.stereotype.Component;

@Component
public class CompteEpargneMapper {

    public CompteEpargneResponse toResponse(CompteEpargne ce) {
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