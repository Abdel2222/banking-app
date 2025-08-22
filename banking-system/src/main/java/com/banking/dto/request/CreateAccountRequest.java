package com.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la création d'un compte bancaire.
 * JSON attendu (exemples) :
 * {
 *   "typeCompte": "COURANT",            // requis: COURANT | EPARGNE | PROFESSIONNEL
 *   "intitule": "Compte courant perso", // optionnel
 *   "devise": "EUR"                     // optionnel (code ISO 4217 sur 3 lettres)
 * }
 */
public class CreateAccountRequest {

    @NotBlank(message = "Le type de compte est obligatoire")
    @Pattern(
            regexp = "^(COURANT|EPARGNE|PROFESSIONNEL)$",
            message = "Le type de compte doit être COURANT, EPARGNE ou PROFESSIONNEL"
    )
    private String typeCompte;

    /** Intitulé facultatif. Exemple: "Compte courant", "Compte pro"... */
    @Size(max = 255, message = "L’intitulé ne doit pas dépasser 255 caractères")
    private String intitule;

    /** Devise facultative (code ISO 4217 sur 3 lettres). Exemple: EUR, USD, MAD... */
    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO à 3 lettres (ex: EUR)")
    private String devise;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String typeCompte, String intitule, String devise) {
        this.typeCompte = typeCompte;
        this.intitule = intitule;
        this.devise = devise;
    }

    // Getters
    public String getTypeCompte() {
        return typeCompte;
    }

    public String getIntitule() {
        return intitule;
    }

    public String getDevise() {
        return devise;
    }

    // Setters
    public void setTypeCompte(String typeCompte) {
        this.typeCompte = typeCompte;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    @Override
    public String toString() {
        return "CreateAccountRequest{" +
                "typeCompte='" + typeCompte + '\'' +
                ", intitule='" + intitule + '\'' +
                ", devise='" + devise + '\'' +
                '}';
    }
}
