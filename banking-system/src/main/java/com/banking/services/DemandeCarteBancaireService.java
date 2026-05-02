package com.banking.services;

import com.banking.entities.DemandeCarteBancaire;

import java.util.List;

public interface DemandeCarteBancaireService {

    DemandeCarteBancaire demanderCarte(Long compteId);


    DemandeCarteBancaire approuverDemande(Long demandeId);

    DemandeCarteBancaire rejeterDemande(Long demandeId, String raison);

    List<DemandeCarteBancaire> listerDemandes();

    List<DemandeCarteBancaire> listerDemandesParClient(Long clientId);
}

