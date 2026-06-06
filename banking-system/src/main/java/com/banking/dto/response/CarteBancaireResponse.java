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
    private String cvv;
    private boolean estActive;
    private Double plafondJournalier;
    private Double plafondMensuel;
    private Long compteId;

    /**
     * Réponse standard — numéro masqué, CVV masqué "•••"
     * Utilisée partout sauf endpoint /mon-cvv
     */
    public static CarteBancaireResponse fromEntity(CarteBancaire c) {
        if (c == null) return null;
        return CarteBancaireResponse.builder()
                .id(c.getId())
                .numeroCarte(masquerNumero(c.getNumeroCarte()))
                .dateExpiration(c.getDateExpiration())
                .cvv("•••")
                .estActive(c.isEstActive())
                .plafondJournalier(c.getPlafondJournalier())
                .plafondMensuel(c.getPlafondMensuel())
                .compteId(null)
                .build();
    }

    /**
     * Réponse avec CVV déchiffré — uniquement pour le client authentifié
     * via l'endpoint GET /api/cartes/{numCompte}/mon-cvv
     */
    public static CarteBancaireResponse fromEntityWithCvv(CarteBancaire c, String cvvDechiffre) {
        if (c == null) return null;
        return CarteBancaireResponse.builder()
                .id(c.getId())
                .numeroCarte(masquerNumero(c.getNumeroCarte()))
                .dateExpiration(c.getDateExpiration())
                .cvv(cvvDechiffre != null ? cvvDechiffre : "•••")
                .estActive(c.isEstActive())
                .plafondJournalier(c.getPlafondJournalier())
                .plafondMensuel(c.getPlafondMensuel())
                .compteId(null)
                .build();
    }

    /**
     * Réponse à l'émission (admin) — CVV clair depuis @Transient
     */
    public static CarteBancaireResponse fromEntityWithCvv(CarteBancaire c) {
        if (c == null) return null;
        return CarteBancaireResponse.builder()
                .id(c.getId())
                .numeroCarte(masquerNumero(c.getNumeroCarte()))
                .dateExpiration(c.getDateExpiration())
                .cvv(c.getCvvClair() != null ? c.getCvvClair() : "•••")
                .estActive(c.isEstActive())
                .plafondJournalier(c.getPlafondJournalier())
                .plafondMensuel(c.getPlafondMensuel())
                .compteId(null)
                .build();
    }

    private static String masquerNumero(String numero) {
        if (numero == null) return "•••• •••• •••• ••••";
        String clean = numero.replaceAll("\\s", "");
        if (clean.length() >= 4) {
            return "•••• •••• •••• " + clean.substring(clean.length() - 4);
        }
        return "•••• •••• •••• ••••";
    }
}