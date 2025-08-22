package com.banking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Requête de création de carte bancaire.
 * Remarques :
 * - Le serveur peut générer numeroCarte et/ou cvv si non fournis.
 * - cvv est désormais sur 3 chiffres (plus de codeCvv).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCarteBancaireRequest {

    /**
     * Numéro de compte auquel lier la carte (obligatoire).
     * Exemple : "ACC1234567890" ou IBAN/numéro interne selon ton modèle.
     */
    @NotBlank(message = "Le numéro de compte est obligatoire")
    private String numeroCompte;

    /**
     * (Optionnel) Numéro de carte à 16 chiffres.
     * Si null, il sera généré côté service.
     */
    @Size(min = 16, max = 16, message = "Le numéro de carte doit contenir 16 chiffres")
    private String numeroCarte;

    /**
     * (Optionnel) Date d'expiration. Si null, par défaut +3 ans.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateExpiration;

    /**
     * (Optionnel) CVV sur 3 chiffres. Si null, il sera généré côté service.
     * (Remplace l'ancien 'codeCvv'.)
     */
    @Size(min = 3, max = 3, message = "Le CVV doit contenir 3 chiffres")
    private String cvv;

    /**
     * (Optionnel) Plafonds. Si null, des valeurs par défaut seront posées côté service.
     */
    private Double plafondJournalier;
    private Double plafondMensuel;

    /**
     * (Optionnel) Activer la carte dès la création. Par défaut : true.
     */
    @Builder.Default
    private Boolean estActive = Boolean.TRUE;
}
