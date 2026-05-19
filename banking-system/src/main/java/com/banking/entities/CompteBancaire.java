package com.banking.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.banking.entity.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Entity
@Table(name = "comptes_bancaires")
@Getter @Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class CompteBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_compte", length = 16, nullable = false, unique = true)
    private String numCompte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.CREATED;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "devise", length = 3)
    private String devise = "EUR";

    @Column(name = "intitule")
    private String intitule;

    @Column(name = "created_at", columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "datetime(6)")
    private LocalDateTime updatedAt;

    /* ===================== Relations ===================== */

    @OneToOne(mappedBy = "compteBancaire", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private CarteBancaire carteBancaire;

    @OneToMany(mappedBy = "compteBancaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Operation> operations = new ArrayList<>();

    @OneToMany(mappedBy = "compteBancaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FraisDeGestion> fraisDeGestions = new ArrayList<>();

    // ❌ interets SUPPRIMÉ — déplacé dans CompteEpargne

    /* ===================== Constructeurs ===================== */

    public CompteBancaire(String numCompte, Client client) {
        this.numCompte = Objects.requireNonNull(numCompte, "numCompte");
        this.client    = Objects.requireNonNull(client, "client");
        this.status    = AccountStatus.CREATED;
        this.balance   = BigDecimal.ZERO;
        this.devise    = "EUR";
        this.intitule  = "Compte courant";
    }

    public static CompteBancaire createNew(Client client, String intitule, String devise) {
        CompteBancaire c = new CompteBancaire();
        c.numCompte = generateAccountNumber();
        c.client    = Objects.requireNonNull(client, "client");
        c.status    = AccountStatus.CREATED;
        c.balance   = BigDecimal.ZERO;
        c.intitule  = (intitule == null || intitule.isBlank()) ? "Compte courant" : intitule;
        c.devise    = (devise   == null || devise.isBlank())   ? "EUR"            : devise;
        return c;
    }

    /* ===================== Règles métier ===================== */

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVATED;
    }

    public void activate() {
        if (this.status != AccountStatus.ACTIVATED)
            this.status = AccountStatus.ACTIVATED;
    }

    public void block()  { this.status = AccountStatus.SUSPENDED; }
    public void close()  { this.status = AccountStatus.SUSPENDED; }

    public void crediter(BigDecimal montant) {
        requirePositive(montant, "montant");
        this.balance = this.balance.add(montant);
    }

    public void debiter(BigDecimal montant) {
        requirePositive(montant, "montant");
        if (this.balance.compareTo(montant) < 0)
            throw new IllegalStateException("Solde insuffisant");
        this.balance = this.balance.subtract(montant);
    }

    /* ===================== Helpers relations ===================== */

    public void attacherCarte(CarteBancaire carte) {
        this.carteBancaire = carte;
        if (carte != null) carte.setCompteBancaire(this);
    }

    public void ajouterOperation(Operation operation) {
        operations.add(operation);
        operation.setCompteBancaire(this);
    }

    public void ajouterFrais(FraisDeGestion frais) {
        fraisDeGestions.add(frais);
        frais.setCompteBancaire(this);
    }

    // ❌ ajouterInteret() SUPPRIMÉ — déplacé dans CompteEpargne

    /* ===================== Getters ===================== */

    public BigDecimal getBalance() {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    /* ===================== Hooks JPA ===================== */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.balance  == null)                            this.balance  = BigDecimal.ZERO;
        if (this.status   == null)                            this.status   = AccountStatus.CREATED;
        if (this.devise   == null || this.devise.isBlank())   this.devise   = "EUR";
        if (this.intitule == null || this.intitule.isBlank()) this.intitule = "Compte courant";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* ===================== Utils ===================== */

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0)
            throw new IllegalArgumentException(field + " doit être strictement positif");
    }

    public static String generateAccountNumber() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    /* ===================== JSON helpers ===================== */

    @Transient
    @JsonProperty("hasCard")
    public boolean isHasCard() {
        return this.carteBancaire != null;
    }

    @Transient
    @JsonProperty("hasSavingsAccount")
    public boolean isHasSavingsAccount() {
        return this instanceof CompteEpargne;
    }
}