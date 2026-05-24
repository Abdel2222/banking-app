package com.banking.entities;

import com.banking.entity.enums.Role;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
public class Client extends Personne {

    // ===== Relations =====

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = false)
    @JsonManagedReference("client-comptes")  // ✅ coupe la récursion côté parent
    private List<CompteBancaire> comptes = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("client-frais")    // ✅ coupe la récursion côté parent
    private List<FraisDeGestion> fraisDeGestion = new ArrayList<>();

    // ===== Constructeurs =====

    public Client() {
        super();
    }

    public Client(String prenom, String nom, String email, String motDePasse) {
        super(prenom, nom, email, motDePasse, Role.CLIENT);
    }

    // ===== Méthodes métier pour CompteBancaire =====

    public void ajouterCompte(CompteBancaire compte) {
        if (compte == null) return;
        if (!comptes.contains(compte)) {
            comptes.add(compte);
            compte.setClient(this);
        }
    }

    public void supprimerCompte(CompteBancaire compte) {
        if (compte == null) return;
        comptes.remove(compte);
    }

    public int getNombreComptes() {
        return comptes == null ? 0 : comptes.size();
    }

    public CompteBancaire getCompteParNumero(String numeroCompte) {
        if (comptes == null) return null;
        return comptes.stream()
                .filter(c -> c != null && numeroCompte != null && numeroCompte.equals(c.getNumCompte()))
                .findFirst()
                .orElse(null);
    }

    public boolean aDesComptes() {
        return comptes != null && !comptes.isEmpty();
    }

    public List<CompteBancaire> getComptesActifs() {
        if (comptes == null) return List.of();
        return comptes.stream()
                .filter(c -> c != null && c.isActive())
                .toList();
    }

    public BigDecimal getSoldeTotal() {
        if (comptes == null) return BigDecimal.ZERO;
        return comptes.stream()
                .filter(Objects::nonNull)
                .map(CompteBancaire::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== Méthodes métier pour FraisDeGestion =====

    public void ajouterFrais(FraisDeGestion frais) {
        if (frais == null) return;
        if (!fraisDeGestion.contains(frais)) {
            fraisDeGestion.add(frais);
            frais.setClient(this);
        }
    }

    public void supprimerFrais(FraisDeGestion frais) {
        if (frais == null) return;
        fraisDeGestion.remove(frais);
    }

    public BigDecimal getTotalFraisActifs() {
        if (fraisDeGestion == null) return BigDecimal.ZERO;
        return fraisDeGestion.stream()
                .filter(f -> f != null && f.estEnCours())
                .map(FraisDeGestion::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalFraisFactures() {
        if (fraisDeGestion == null) return BigDecimal.ZERO;
        return fraisDeGestion.stream()
                .filter(Objects::nonNull)
                .map(FraisDeGestion::getMontantTotalFacture)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<FraisDeGestion> getFraisEnCours() {
        if (fraisDeGestion == null) return List.of();
        return fraisDeGestion.stream()
                .filter(f -> f != null && f.estEnCours())
                .toList();
    }

    public List<FraisDeGestion> getFraisEchus() {
        if (fraisDeGestion == null) return List.of();
        return fraisDeGestion.stream()
                .filter(f -> f != null && f.estEchu())
                .toList();
    }

    public boolean aDesFraisImpayes() {
        if (fraisDeGestion == null) return false;
        return fraisDeGestion.stream()
                .anyMatch(f -> f != null && f.doitEtreFacture() && f.getEstActif());
    }

    // ===== Getters / Setters =====

    public List<CompteBancaire> getComptes() { return comptes; }
    public void setComptes(List<CompteBancaire> comptes) { this.comptes = comptes; }

    public List<FraisDeGestion> getFraisDeGestion() { return fraisDeGestion; }
    public void setFraisDeGestion(List<FraisDeGestion> fraisDeGestion) { this.fraisDeGestion = fraisDeGestion; }

    // ===== toString / equals / hashCode =====

    @Override
    public String toString() {
        return "Client{" +
                "id=" + getId() +
                ", nom='" + getNomComplet() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", nombreComptes=" + getNombreComptes() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client other)) return false;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}