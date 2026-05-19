package com.banking.services;

import com.banking.entities.Interet;
import java.math.BigDecimal;
import java.util.List;

public interface InteretService {

    // Créer un intérêt pour un compte épargne
    Interet creer(String numCompteEpargne, BigDecimal tauxInteret);

    // Calculer les intérêts sans capitaliser
    BigDecimal calculer(String numCompteEpargne);

    // Capitaliser — ajoute les intérêts au solde
    Interet capitaliser(String numCompteEpargne);

    // Vérifier si la capitalisation est due
    boolean doitCapitaliser(String numCompteEpargne);

    // Historique des intérêts
    List<Interet> historique(String numCompteEpargne);

    // Changer le taux
    Interet updateTaux(String numCompteEpargne, BigDecimal nouveauTaux);
}