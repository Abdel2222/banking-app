package com.banking.services;

import com.banking.entities.Personne;

import java.util.List;

public interface AdminService {

    long countAdmins();

    Personne createAdmin(String prenom, String nom, String email, String rawPassword);

    Personne createSuperAdmin(String prenom, String nom, String email, String rawPassword);

    Personne promoteToAdmin(Long userId);

    Personne demoteToClient(Long userId); // optionnel

    List<Personne> listAdmins();
}

