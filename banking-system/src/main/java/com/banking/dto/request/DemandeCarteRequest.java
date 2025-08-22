package com.banking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemandeCarteRequest {
    @NotNull
    private Long compteId;
}
