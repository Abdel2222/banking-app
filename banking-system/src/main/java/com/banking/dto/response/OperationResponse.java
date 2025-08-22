package com.banking.dto.response;

import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OperationResponse {
    private Long id;
    private LocalDateTime dateOperation;
    private BigDecimal montant;
    private TypeOperation type;
    private String description;               // lit description, fallback commentaire
    private String numeroCompte;
    private String numeroCompteDestinataire;
    private String communication;
    private String nomTitulaireDestinataire;  // utile pour virements

    public OperationResponse(Operation op) {
        this.id = op.getId();
        this.dateOperation = op.getDateOperation();
        this.montant = op.getMontant();
        this.type = op.getType(); // alias vers typeOperation
        // 👉 priorité à description, sinon commentaire
        this.description = (op.getDescription() != null && !op.getDescription().isBlank())
                ? op.getDescription()
                : op.getCommentaire();
        this.numeroCompte = op.getNumeroCompte();
        this.numeroCompteDestinataire = op.getNumeroCompteDestinataire();
        this.communication = op.getCommunication();
        this.nomTitulaireDestinataire = op.getNomTitulaireDestinataire();
    }

    public static OperationResponse fromEntity(Operation op) {
        return op == null ? null : new OperationResponse(op);
    }

    public static List<OperationResponse> fromEntities(List<Operation> ops) {
        if (ops == null || ops.isEmpty()) return List.of();
        return ops.stream().filter(Objects::nonNull)
                .map(OperationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public LocalDateTime getDateOperation() { return dateOperation; }
    public BigDecimal getMontant() { return montant; }
    public TypeOperation getType() { return type; }
    public String getDescription() { return description; }
    public String getNumeroCompte() { return numeroCompte; }
    public String getNumeroCompteDestinataire() { return numeroCompteDestinataire; }
    public String getCommunication() { return communication; }
    public String getNomTitulaireDestinataire() { return nomTitulaireDestinataire; }
}
