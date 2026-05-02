package com.banking.services.impl;

import com.banking.entities.Operation;
import com.banking.entities.ReleveDeCompte;
import com.banking.entities.ReleveDeCompte;
import com.banking.repositories.ReleveDeCompteRepository;
import com.banking.services.ReleveDeCompteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class ReleveDeCompteServiceImpl implements ReleveDeCompteService {

    private final ReleveDeCompteRepository repo;

    public ReleveDeCompteServiceImpl(ReleveDeCompteRepository repo) {
        this.repo = repo;
    }

    @Override
    public void logOperation(Operation op, BigDecimal soldeApres) {
        ReleveDeCompte l = new ReleveDeCompte();
        l.setNumCompte(op.getNumeroCompte());                          // ou op.getCompteBancaire().getNumCompte()
        l.setTypeOperation(op.getTypeOperation());
        l.setMontant(op.getMontant());
        l.setDescription(op.getDescription());
        l.setDateOperation(op.getDateOperation());
        l.setSolde(soldeApres);
        l.setCompte(op.getCompteBancaire());
        repo.save(l);
    }
}
