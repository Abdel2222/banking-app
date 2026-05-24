package com.banking.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ChatCreateRequest {

    @NotBlank
    private String contenu;

    /** Optionnel : ID du client (sinon récupéré du JWT côté backend) */
    private Long clientId;

    /** Optionnel : nom du client (pour affichage admin) */
    private String clientNom;

    /** Optionnel : compte concerné par la demande */
    private String numCompte;

    public ChatCreateRequest() {}

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getNumCompte() { return numCompte; }
    public void setNumCompte(String numCompte) { this.numCompte = numCompte; }
}