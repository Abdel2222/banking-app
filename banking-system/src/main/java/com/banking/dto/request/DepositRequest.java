package com.banking.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {

    // IBAN (ex: BE...), ou identifiant alphanumérique (ex: ACC000014), 6–34 chars
    private static final String ACCOUNT_REGEX =
            "^([A-Z]{2}\\d{2}[A-Z0-9]{11,30}|[A-Z0-9]{6,34})$";

    @NotBlank(message = "Le numéro de compte est obligatoire")
    @Pattern(
            regexp = ACCOUNT_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "Numéro de compte invalide (IBAN ou alphanumérique 6–34)"
    )
    @JsonAlias({"numCompte", "accountNumber", "compte"})
    private String numCompte;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    private String description;
}
