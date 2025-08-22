package com.banking.dto.response;

import java.math.BigDecimal;

/**
 * DTO pour les statistiques des frais d'un client
 */
public class FraisStatistiquesResponse {

    private Long nombreFraisActifs;
    private BigDecimal montantTotalActif;
    private BigDecimal montantTotalFacture;
    private BigDecimal moyenneMontantFrais;
    private Long nombreFacturationsTotal;

    // Constructeurs
    public FraisStatistiquesResponse() {}

    public FraisStatistiquesResponse(Long nombreFraisActifs, BigDecimal montantTotalActif,
                                     BigDecimal montantTotalFacture, BigDecimal moyenneMontantFrais) {
        this.nombreFraisActifs = nombreFraisActifs;
        this.montantTotalActif = montantTotalActif;
        this.montantTotalFacture = montantTotalFacture;
        this.moyenneMontantFrais = moyenneMontantFrais;
    }

    // Getters et Setters
    public Long getNombreFraisActifs() { return nombreFraisActifs; }
    public void setNombreFraisActifs(Long nombreFraisActifs) { this.nombreFraisActifs = nombreFraisActifs; }

    public BigDecimal getMontantTotalActif() { return montantTotalActif; }
    public void setMontantTotalActif(BigDecimal montantTotalActif) { this.montantTotalActif = montantTotalActif; }

    public BigDecimal getMontantTotalFacture() { return montantTotalFacture; }
    public void setMontantTotalFacture(BigDecimal montantTotalFacture) { this.montantTotalFacture = montantTotalFacture; }

    public BigDecimal getMoyenneMontantFrais() { return moyenneMontantFrais; }
    public void setMoyenneMontantFrais(BigDecimal moyenneMontantFrais) { this.moyenneMontantFrais = moyenneMontantFrais; }

    public Long getNombreFacturationsTotal() { return nombreFacturationsTotal; }
    public void setNombreFacturationsTotal(Long nombreFacturationsTotal) { this.nombreFacturationsTotal = nombreFacturationsTotal; }
}
