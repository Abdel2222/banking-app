package com.banking.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EpargneConvertRequest {
    // ❌ private BigDecimal tauxInteret;
    private BigDecimal premierMontant;  // ✅ nouveau champ
}