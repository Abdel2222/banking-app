package com.banking.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EpargneMontantRequest {
    private BigDecimal montant;
    private String numCompteSource; // ← nouveau champ pour virement interne
}