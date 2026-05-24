package com.banking.entity.enums;

public enum TypeOperation {
    CREDIT,
    DEBIT,
    DEPOT,
    RETRAIT,
    VIREMENT,
    FRAIS,
    INTERETS,
    AJUSTEMENT,
    BLOCAGE_CARTE,
    PLACEMENT,           // ✅ débit lors d'un placement
    CLOTURE_PLACEMENT,   // ✅ crédit capital + gain à échéance
    SORTIE_ANTICIPEE,    // ✅ crédit montant restitué avant échéance
    FRAIS_SORTIE,        // ✅ débit frais lors d'une sortie anticipée
    ANNULATION_PLACEMENT // ✅ crédit remboursement sans frais
}