package com.banking.dto.response;

import com.banking.entities.ReleveDeCompte;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReleveDto(
        LocalDateTime date,
        String type,
        BigDecimal montant,
        String description,
        BigDecimal solde
) {
    public static ReleveDto from(ReleveDeCompte l) {
        return new ReleveDto(
                l.getDateOperation(),
                l.getTypeOperation() != null ? l.getTypeOperation().name() : null,
                l.getMontant(),
                l.getDescription(),
                l.getSolde()
        );
    }
}
