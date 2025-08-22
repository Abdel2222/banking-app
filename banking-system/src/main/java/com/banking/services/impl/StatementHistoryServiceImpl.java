package com.banking.services.impl;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entities.ReleveDeCompte;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.ReleveDeCompteRepository;
import com.banking.services.OperationService;
import com.banking.services.StatementHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class StatementHistoryServiceImpl implements StatementHistoryService {

    private final CompteBancaireRepository compteRepo;
    private final ReleveDeCompteRepository releveRepo;
    private final OperationService operationService;

    public StatementHistoryServiceImpl(CompteBancaireRepository compteRepo,
                                       ReleveDeCompteRepository releveRepo,
                                       OperationService operationService) {
        this.compteRepo = compteRepo;
        this.releveRepo = releveRepo;
        this.operationService = operationService;
    }

    @Override
    public ReleveDeCompte persistMonthly(String numCompte, int year, int month) {
        CompteBancaire compte = compteRepo.findByNumCompte(numCompte)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable: " + numCompte));

        // Évite les doublons : un relevé/mois/compte
        releveRepo.findByCompte_IdAndAnneeAndMois(compte.getId(), year, month)
                .ifPresent(r -> { throw new IllegalStateException("Relevé déjà existant pour " + month + "/" + year); });

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to   = end.atTime(23,59,59);

        // Récupère les opérations du mois
        List<Operation> ops = operationService.findByAccountAndDateRange(numCompte, from, to);

        // Totalise
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits  = BigDecimal.ZERO;
        for (Operation o : ops) {
            if (o.getMontant() == null) continue;
            switch (o.getTypeOperation()) {
                case DEPOT, CREDIT, INTERETS -> credits = credits.add(o.getMontant());
                case RETRAIT, DEBIT, FRAIS, VIREMENT -> debits = debits.add(o.getMontant());
                default -> {} // BLOCAGE_CARTE, AJUSTEMENT, etc.
            }
        }

        // Solde final = balance actuelle (colonne "balance" en BDD)
        BigDecimal soldeFinal = compte.getBalance() == null ? BigDecimal.ZERO : compte.getBalance();
        // Solde initial = solde final - (crédits - débits) du mois
        BigDecimal soldeInitial = soldeFinal.subtract(credits.subtract(debits));

        ReleveDeCompte r = new ReleveDeCompte();
        r.setCompte(compte);
        r.setAnnee(year);
        r.setMois(month);
        r.setDateReleve(end);           // au dernier jour du mois
        r.setNombreOperations(ops.size());
        r.setTotalCredits(credits);
        r.setTotalDebits(debits);
        r.setSoldeInitial(soldeInitial.max(BigDecimal.ZERO)); // sécurité si négatif
        r.setSoldeFinal(soldeFinal);
        r.setPdfGenere(false);
        // r.setCheminPdf(...); // si tu stockes un fichier physique

        return releveRepo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReleveDeCompte> listHistory(String numCompte) {
        return releveRepo.findByCompte_NumCompteOrderByDateReleveDesc(numCompte);
    }
}
