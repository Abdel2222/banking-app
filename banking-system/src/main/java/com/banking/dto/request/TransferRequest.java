package com.banking.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    // ✅ Nouveau regex : accepte comptes numériques entre 6 et 34 chiffres
    private static final String ACCOUNT_REGEX = "^[0-9]{6,34}$";

    @NotBlank(message = "Le compte source est obligatoire")
    @Pattern(
            regexp = ACCOUNT_REGEX,
            message = "Numéro de compte source invalide"
    )
    @JsonAlias({"fromAccount", "sourceAccount", "compteSource"})
    private String numCompteSource;

    @NotBlank(message = "Le compte destinataire est obligatoire")
    @Pattern(
            regexp = ACCOUNT_REGEX,
            message = "Numéro de compte destinataire invalide"
    )
    @JsonAlias({"toAccount", "destinationAccount", "compteDestinataire"})
    private String numCompteDestinataire;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    private String communication;
}
