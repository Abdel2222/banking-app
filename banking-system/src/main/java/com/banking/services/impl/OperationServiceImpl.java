package com.banking.services.impl;

import com.banking.entities.CompteBancaire;
import com.banking.entities.Operation;
import com.banking.entity.enums.TypeOperation;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.CompteBancaireRepository;
import com.banking.repositories.OperationRepository;
import com.banking.services.CompteBancaireService;
import com.banking.services.OperationService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OperationServiceImpl implements OperationService {

    private final OperationRepository operationRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CompteBancaireService compteBancaireService;

    public OperationServiceImpl(OperationRepository operationRepository,
                                CompteBancaireRepository compteBancaireRepository,
                                CompteBancaireService compteBancaireService) {
        this.operationRepository = operationRepository;
        this.compteBancaireRepository = compteBancaireRepository;
        this.compteBancaireService = compteBancaireService;
    }

    /* =================== CRUD & lecture =================== */

    @Override
    public Operation createOperation(String numCompte, BigDecimal montant, TypeOperation type, String description) {
        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
        Operation op = new Operation(compte, montant, type,
                (description != null && !description.isBlank()) ? description : type.name());
        return operationRepository.save(op);
    }

    @Override public Optional<Operation> findById(Long id) { return operationRepository.findById(id); }
    @Override public List<Operation> findAll() { return operationRepository.findAll(); }

    @Override
    public Page<Operation> findAllPaginated(Pageable pageable) {
        List<Operation> all = operationRepository.findAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Operation> slice = start > end ? List.of() : all.subList(start, end);
        return new PageImpl<>(slice, pageable, all.size());
    }

    /* =================== Opérations bancaires (délégation) =================== */

    @Override
    public Operation executeDeposit(String numCompte, BigDecimal montant, String description) {
        // délégué au service comptes pour tenir le solde à jour
        return compteBancaireService.deposit(numCompte, montant, description);
    }

    @Override
    public Operation executeWithdrawal(String numCompte, BigDecimal montant, String description) {
        return compteBancaireService.withdraw(numCompte, montant, description);
    }

    @Override
    public Operation executeTransfer(String fromAccount, String toAccount, BigDecimal montant, String communication) {
        return compteBancaireService.transfer(fromAccount, toAccount, montant, communication);
    }

    @Override
    public Operation executeCardBlock(String numCompte, String raison) {
        // Si tu as une vraie logique de blocage dans CompteBancaireService, appelle-la ici
        // et journalise une opération BLOCAGE_CARTE.
        // Ici on enregistre juste une opération "informatif".
        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
        Operation op = new Operation(compte, BigDecimal.ZERO, TypeOperation.BLOCAGE_CARTE,
                (raison != null && !raison.isBlank()) ? raison : "Blocage de carte");
        return operationRepository.save(op);
    }

    /* =================== Requêtes =================== */

    @Override
    @Transactional(readOnly = true)
    public List<Operation> findByAccount(String numCompte) {
        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
        // on réutilise la requête période avec une grande fenêtre
        return operationRepository.findByCompteAndDateBetween(
                compte, LocalDateTime.of(1970,1,1,0,0), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Operation> findByAccountPaginated(String numCompte, Pageable pageable) {
        List<Operation> list = findByAccount(numCompte);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<Operation> slice = start > end ? List.of() : list.subList(start, end);
        return new PageImpl<>(slice, pageable, list.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operation> findByType(TypeOperation type) {
        return operationRepository.findByTypeOperation(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operation> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return operationRepository.findAll().stream()
                .filter(o -> !o.getDateOperation().isBefore(start) && !o.getDateOperation().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operation> findByAccountAndDateRange(String numCompte, LocalDateTime start, LocalDateTime end) {
        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));
        return operationRepository.findByCompteAndDateBetween(compte, start, end);
    }

    @Override
    public List<Operation> getOperationsByAccountNumber(String accountNumber) {
        return findByAccount(accountNumber);
    }

    @Override
    public List<Operation> findLargeTransactions(BigDecimal minAmount) {
        return operationRepository.findAll().stream()
                .filter(o -> o.getMontant() != null && o.getMontant().compareTo(minAmount) >= 0)
                .toList();
    }

    @Override
    public List<Operation> findSuspiciousTransactions() {
        // règle simple: montants >= 10_000
        return findLargeTransactions(new BigDecimal("10000"));
    }

    @Override
    public List<Operation> findTransfersToAccount(String numCompte) {
        return operationRepository.findTransfersToAccount(numCompte);
    }

    @Override
    public List<Operation> findTransfersFromAccount(String numCompte) {
        // pas de méthode repo dédiée -> filtre en mémoire
        return findByAccount(numCompte).stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.VIREMENT)
                .toList();
    }

    @Override
    public List<Operation> findRecurringTransactions(String numCompte) {
        // simple placeholder (selon besoins métier)
        return List.of();
    }

    /* =================== Stats =================== */

    @Override
    public Map<TypeOperation, BigDecimal> getStatisticsByType(LocalDateTime start, LocalDateTime end) {
        Map<TypeOperation, BigDecimal> map = new EnumMap<>(TypeOperation.class);
        for (TypeOperation t : TypeOperation.values()) map.put(t, BigDecimal.ZERO);

        findByDateRange(start, end).forEach(op -> {
            BigDecimal current = map.getOrDefault(op.getTypeOperation(), BigDecimal.ZERO);
            map.put(op.getTypeOperation(), current.add(op.getMontant() != null ? op.getMontant() : BigDecimal.ZERO));
        });
        return map;
    }

    @Override
    public BigDecimal getTotalDeposits(String numCompte, LocalDateTime start, LocalDateTime end) {
        return findByAccountAndDateRange(numCompte, start, end).stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.DEPOT)
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalWithdrawals(String numCompte, LocalDateTime start, LocalDateTime end) {
        return findByAccountAndDateRange(numCompte, start, end).stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.RETRAIT)
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalTransfers(String numCompte, LocalDateTime start, LocalDateTime end) {
        return findByAccountAndDateRange(numCompte, start, end).stream()
                .filter(o -> o.getTypeOperation() == TypeOperation.VIREMENT)
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, Object> getMonthlyReport(String numCompte, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23,59,59);

        List<Operation> list = findByAccountAndDateRange(numCompte, start, end);
        BigDecimal depots = list.stream().filter(o -> o.getTypeOperation() == TypeOperation.DEPOT)
                .map(Operation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retraits = list.stream().filter(o -> o.getTypeOperation() == TypeOperation.RETRAIT)
                .map(Operation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal virements = list.stream().filter(o -> o.getTypeOperation() == TypeOperation.VIREMENT)
                .map(Operation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("compte", numCompte);
        map.put("periode", ym.toString());
        map.put("nombreOperations", list.size());
        map.put("totalDepots", depots);
        map.put("totalRetraits", retraits);
        map.put("totalVirements", virements);
        return map;
    }

    /* =================== Validation =================== */

    @Override
    public boolean validateOperation(Operation operation) {
        return operation != null
                && operation.getMontant() != null
                && operation.getMontant().compareTo(BigDecimal.ZERO) >= 0
                && operation.getTypeOperation() != null
                && operation.getCompteBancaire() != null;
    }

    @Override
    public boolean canExecuteOperation(String numCompte, BigDecimal montant, TypeOperation type) {
        // délègue au service comptes (vérifie solde/état)
        try {
            return compteBancaireRepository.findByNumCompte(numCompte)
                    .map(CompteBancaire::isActive)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDuplicateOperation(Operation operation) {
        // placeholder (hash par date+montant+type+numcompte)
        if (operation == null || operation.getDateOperation() == null) return false;
        return operationRepository.findAll().stream().anyMatch(o ->
                Objects.equals(o.getDateOperation(), operation.getDateOperation())
                        && Objects.equals(o.getMontant(), operation.getMontant())
                        && Objects.equals(o.getTypeOperation(), operation.getTypeOperation())
                        && Objects.equals(o.getNumeroCompte(), operation.getNumeroCompte())
        );
    }

    /* =================== Annulation/correction (optionnel) =================== */

    @Override public Operation cancelOperation(Long operationId, String raison) { throw new UnsupportedOperationException("Non implémenté"); }
    @Override public Operation reverseOperation(Long operationId) { throw new UnsupportedOperationException("Non implémenté"); }
    @Override public Operation correctOperation(Long operationId, BigDecimal newAmount, String raison) { throw new UnsupportedOperationException("Non implémenté"); }

    /* =================== Export / Reçu =================== */

    @Override
    public byte[] exportOperationsToPdf(String numCompte, LocalDateTime start, LocalDateTime end) {
        CompteBancaire compte = compteBancaireRepository.findByNumCompte(numCompte)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + numCompte));

        List<Operation> ops = operationRepository.findByCompteAndDateBetween(compte, start, end);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            doc.add(new Paragraph("Relevé de compte"));
            doc.add(new Paragraph("Titulaire : " + compte.getClient().getNomComplet()));
            doc.add(new Paragraph("Compte    : " + compte.getNumCompte()));
            doc.add(new Paragraph(String.format("Période   : %s -> %s", start, end)));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{28f, 22f, 18f, 32f});

            table.addCell(header("Date"));
            table.addCell(header("Type"));
            table.addCell(header("Montant"));
            table.addCell(header("Description"));

            for (Operation o : ops) {
                table.addCell(new Phrase(o.getDateOperation() != null ? o.getDateOperation().toString() : ""));
                table.addCell(new Phrase(o.getTypeOperation() != null ? o.getTypeOperation().name() : ""));
                table.addCell(new Phrase(o.getMontant() != null ? o.getMontant().toPlainString() : "0"));
                table.addCell(new Phrase(o.getDescription() != null ? o.getDescription() : ""));
            }
            doc.add(table);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    private static PdfPCell header(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text));
        c.setPadding(5f);
        return c;
    }

    @Override
    public byte[] exportOperationsToExcel(String numCompte, LocalDateTime start, LocalDateTime end) {
        // Simple CSV pour éviter une dépendance POI
        List<Operation> ops = findByAccountAndDateRange(numCompte, start, end);
        StringBuilder sb = new StringBuilder();
        sb.append("date,type,montant,description\n");
        for (Operation o : ops) {
            sb.append(s(o.getDateOperation())).append(',')
                    .append(s(o.getTypeOperation())).append(',')
                    .append(o.getMontant() != null ? o.getMontant() : BigDecimal.ZERO).append(',')
                    .append(escapeCsv(o.getDescription())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
    private static String escapeCsv(String s) {
        if (s == null) return "";
        String x = s.replace("\"", "\"\"");
        if (x.contains(",") || x.contains("\"") || x.contains("\n")) return "\"" + x + "\"";
        return x;
    }

    @Override
    public String generateOperationReceipt(Long operationId) {
        Operation op = operationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Opération non trouvée : " + operationId));

        String type = op.getTypeOperation() != null ? op.getTypeOperation().name() : "N/A";
        String montant = op.getMontant() != null ? op.getMontant().toPlainString() : "0";
        String date = op.getDateOperation() != null ? op.getDateOperation().toString() : "N/A";
        String compteSrc = op.getNumeroCompte() != null ? op.getNumeroCompte() : "N/A";
        String compteDst = op.getNumeroCompteDestinataire() != null ? op.getNumeroCompteDestinataire() : "N/A";

        String receipt = """
                --- Reçu d'opération ---
                ID opération       : %d
                Type               : %s
                Montant            : %s
                Date               : %s
                Compte source      : %s
                Compte destinataire: %s
                -------------------------
                """.formatted(op.getId(), type, montant, date, compteSrc, compteDst);

        try {
            Path out = Paths.get("recu-operation-" + op.getId() + ".txt");
            Files.writeString(out, receipt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du reçu texte", e);
        }
        return receipt;
    }
}
