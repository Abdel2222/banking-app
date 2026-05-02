package com.banking.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO d'inscription client.
 * Accepte "motDePasse" (nom interne) ET "password" (alias JSON) pour éviter les 400 côté clients.
 */
public class ClientRegistrationRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @Email(message = "L'email doit être valide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @JsonAlias({"password"}) // <-- accepte aussi "password" dans le JSON entrant
    private String motDePasse;

    // --- champs optionnels : décommente si tu les utilises dans ton projet ---
    // private String telephone;
    // private String adresse;

    // Getters / Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    // public String getTelephone() { return telephone; }
    // public void setTelephone(String telephone) { this.telephone = telephone; }

    // public String getAdresse() { return adresse; }
    // public void setAdresse(String adresse) { this.adresse = adresse; }
}
