package com.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReleveDeCompteResponse {

    private Long id;
    private String numCompte;
    private String nomCompletClient;
    private int mois;
    private int annee;
    private YearMonth periode; // juste pour l'IHM; ne pas créer ce champ dans l'entité JPA

    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;

    private BigDecimal montantTotalDepots;
    private BigDecimal montantTotalRetraits;
    private BigDecimal montantTotalVirements;

    private int nombreOperations;
    private LocalDate dateReleve;
    private boolean pdfGenere;

    public ReleveDeCompteResponse() {
        // no-args pour les sérialiseurs (Jackson)
    }

    public ReleveDeCompteResponse(Long id, String numCompte, String nomCompletClient,
                                  int mois, int annee, YearMonth periode,
                                  BigDecimal soldeInitial, BigDecimal soldeFinal,
                                  BigDecimal montantTotalDepots, BigDecimal montantTotalRetraits,
                                  BigDecimal montantTotalVirements, int nombreOperations,
                                  LocalDate dateReleve, boolean pdfGenere) {
        this.id = id;
        this.numCompte = numCompte;
        this.nomCompletClient = nomCompletClient;
        this.mois = mois;
        this.annee = annee;
        this.periode = periode;
        this.soldeInitial = soldeInitial;
        this.soldeFinal = soldeFinal;
        this.montantTotalDepots = montantTotalDepots;
        this.montantTotalRetraits = montantTotalRetraits;
        this.montantTotalVirements = montantTotalVirements;
        this.nombreOperations = nombreOperations;
        this.dateReleve = dateReleve;
        this.pdfGenere = pdfGenere;
    }

    // Getters / Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumCompte() { return numCompte; }
    public void setNumCompte(String numCompte) { this.numCompte = numCompte; }

    public String getNomCompletClient() { return nomCompletClient; }
    public void setNomCompletClient(String nomCompletClient) { this.nomCompletClient = nomCompletClient; }

    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public YearMonth getPeriode() { return periode; }
    public void setPeriode(YearMonth periode) { this.periode = periode; }

    public BigDecimal getSoldeInitial() { return soldeInitial; }
    public void setSoldeInitial(BigDecimal soldeInitial) { this.soldeInitial = soldeInitial; }

    public BigDecimal getSoldeFinal() { return soldeFinal; }
    public void setSoldeFinal(BigDecimal soldeFinal) { this.soldeFinal = soldeFinal; }

    public BigDecimal getMontantTotalDepots() { return montantTotalDepots; }
    public void setMontantTotalDepots(BigDecimal montantTotalDepots) { this.montantTotalDepots = montantTotalDepots; }

    public BigDecimal getMontantTotalRetraits() { return montantTotalRetraits; }
    public void setMontantTotalRetraits(BigDecimal montantTotalRetraits) { this.montantTotalRetraits = montantTotalRetraits; }

    public BigDecimal getMontantTotalVirements() { return montantTotalVirements; }
    public void setMontantTotalVirements(BigDecimal montantTotalVirements) { this.montantTotalVirements = montantTotalVirements; }

    public int getNombreOperations() { return nombreOperations; }
    public void setNombreOperations(int nombreOperations) { this.nombreOperations = nombreOperations; }

    public LocalDate getDateReleve() { return dateReleve; }
    public void setDateReleve(LocalDate dateReleve) { this.dateReleve = dateReleve; }

    public boolean isPdfGenere() { return pdfGenere; }
    public void setPdfGenere(boolean pdfGenere) { this.pdfGenere = pdfGenere; }
}
