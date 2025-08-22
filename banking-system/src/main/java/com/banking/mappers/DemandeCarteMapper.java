package com.banking.mappers;

import com.banking.dto.response.DemandeCarteResponse;
import com.banking.entities.DemandeCarteBancaire;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemandeCarteMapper {

    public DemandeCarteResponse toResponse(DemandeCarteBancaire demande) {
        return DemandeCarteResponse.builder()
                .id(demande.getId())
                .clientId(demande.getClient().getId())
                .compteId(demande.getCompte().getId())
                .status(demande.getStatus())
                .requestedAt(demande.getRequestedAt())
                .rejectedReason(demande.getRejectedReason()) // null si non rejetée
                .build();
    }

    public List<DemandeCarteResponse> toResponseList(List<DemandeCarteBancaire> demandes) {
        return demandes.stream().map(this::toResponse).toList();
    }
}

