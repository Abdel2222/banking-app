package com.banking.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReleveDeCompteResponse {
    private String numeroCompte;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;
    private BigDecimal variation; // ✅ ajouté pour corriger l'erreur
    private List<OperationDto> operations;

    @Data
    public static class OperationDto {
        private LocalDate dateOperation;
        private String type; // DEPOT, RETRAIT, VIREMENT
        private BigDecimal montant;
        private String description;
    }
}

