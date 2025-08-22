package com.banking.services.impl;

import com.banking.entities.Operation;
import com.banking.services.OperationService;
import com.banking.services.ReleveDeCompteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class ReleveDeCompteServiceImpl implements ReleveDeCompteService {

    private final OperationService operationService;

    public ReleveDeCompteServiceImpl(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public byte[] buildStatement(String numCompte, LocalDate start, LocalDate end) {
        if (numCompte == null || numCompte.isBlank()) {
            throw new IllegalArgumentException("numCompte est obligatoire");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Les dates start et end sont obligatoires");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin doit être >= la date de début");
        }

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to   = end.atTime(23, 59, 59);

        // Délègue au service des opérations (OpenPDF)
        return operationService.exportOperationsToPdf(numCompte, from, to);
    }

    @Override
    public byte[] buildMonthlyStatement(String numCompte, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        return buildStatement(numCompte, start, end);
    }

    @Override
    public String generatePlainTextStatement(String numCompte, LocalDateTime from, LocalDateTime to) {
        if (numCompte == null || numCompte.isBlank()) {
            throw new IllegalArgumentException("numCompte est obligatoire");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Les bornes from/to sont obligatoires");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La borne 'to' doit être >= 'from'");
        }

        List<Operation> ops = operationService.findByAccountAndDateRange(numCompte, from, to);

        StringBuilder sb = new StringBuilder(256);
        sb.append("--- Relevé de compte (TEXTE) ---\n")
                .append("Compte : ").append(numCompte).append('\n')
                .append("Période: ").append(from).append(" -> ").append(to).append('\n')
                .append("--------------------------------\n");

        for (Operation o : ops) {
            String date  = o.getDateOperation() != null ? o.getDateOperation().toString() : "";
            String type  = o.getTypeOperation() != null ? o.getTypeOperation().name() : "";
            String mnt   = o.getMontant() != null ? o.getMontant().toPlainString() : BigDecimal.ZERO.toPlainString();
            String desc  = (o.getDescription() != null && !o.getDescription().isBlank())
                    ? o.getDescription()
                    : (o.getCommunication() != null ? o.getCommunication() : "");
            sb.append(date).append(" | ").append(type).append(" | ").append(mnt).append(" | ").append(desc).append('\n');
        }

        sb.append("--------------------------------\n");
        sb.append("Total opérations: ").append(ops.size()).append('\n');
        return sb.toString();
    }
}
