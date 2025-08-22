package com.banking.dto.response;

import com.banking.entities.CarteBancaire;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarteBancaireResponse {
    private Long id;
    private String numeroCarte;
    private LocalDate dateExpiration;
    private String cvv;                // retire-le si tu ne veux jamais exposer le CVV
    private boolean estActive;
    private Double plafondJournalier;
    private Double plafondMensuel;
    private Long compteId;

    // CarteBancaireResponse.java
    public static CarteBancaireResponse fromEntity(CarteBancaire c) {
        if (c == null) return null;
        return CarteBancaireResponse.builder()
                .id(c.getId())
                .numeroCarte(c.getNumeroCarte())
                .dateExpiration(c.getDateExpiration())
                // .cvv(c.getCvv())   // ❌ ne pas exposer
                .cvv(null)            // ✅ masque le CVV
                .estActive(c.isEstActive())
                .plafondJournalier(c.getPlafondJournalier())
                .plafondMensuel(c.getPlafondMensuel())
                // .compteId(c.getCompteBancaire() != null ? c.getCompteBancaire().getId() : null) // ❌ LAZY
                .compteId(null)        // ✅ évite l’accès à la relation LAZY
                .build();
    }
}
