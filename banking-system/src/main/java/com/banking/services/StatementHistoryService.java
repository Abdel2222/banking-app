package com.banking.services;

import com.banking.entities.ReleveDeCompte;

import java.util.List;

public interface StatementHistoryService {
    ReleveDeCompte persistMonthly(String numCompte, int year, int month);
    List<ReleveDeCompte> listHistory(String numCompte);
}
