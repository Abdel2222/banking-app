package com.banking.controllers;

import com.banking.dto.request.*;
import com.banking.dto.response.*;
import com.banking.entities.Client;
import com.banking.services.ClientService;
import com.banking.exceptions.DuplicateResourceException;
import com.banking.exceptions.UnauthorizedAccessException;
import com.banking.security.jwt.JwtTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired private ClientService clientService;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ClientResponse>> register(
            @Valid @RequestBody ClientRegistrationRequest request) {

        logger.info("Tentative d'inscription pour {}", request.getEmail());

        if (clientService.emailExists(request.getEmail())) {
            throw new DuplicateResourceException("Client", "email", request.getEmail());
        }

        Client client = clientService.createClient(
                request.getPrenom(),
                request.getNom(),
                request.getEmail(),
                request.getMotDePasse()
        );

        ClientResponse response = new ClientResponse(client);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inscription réussie", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        logger.info("Tentative de connexion pour {}", request.getEmail());

        Optional<Client> clientOpt = clientService.authenticate(
                request.getEmail(),
                request.getMotDePasse()
        );

        if (clientOpt.isEmpty()) {
            logger.warn("Échec de connexion: {}", request.getEmail());
            throw new UnauthorizedAccessException("Email ou mot de passe incorrect");
        }

        Client client = clientOpt.get();

        String token = jwtTokenProvider.generateToken(
                client.getId(),
                client.getEmail(),
                client.getNomComplet(),
                client.getRole().name()  // ✅ Conversion enum → String
        );

        LoginResponse response = new LoginResponse(
                token,
                client.getId(),
                client.getEmail(),
                client.getNomComplet(),
                client.getRole().name()  // ✅ Converti enum → String
        );


        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout() {
        return ResponseEntity.ok(
                ApiResponse.success("Déconnexion réussie", new MessageResponse("Vous êtes déconnecté"))
        );
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = clientService.emailExists(email);
        return ResponseEntity.ok(ApiResponse.success(exists ? "Email déjà utilisé" : "Email disponible", !exists));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(@RequestBody EmailRequest request) {
        // TODO: si client existe → envoyer un mail de reset
        return ResponseEntity.ok(ApiResponse.success(
                "Si cet email existe, un lien de réinitialisation a été envoyé",
                new MessageResponse("Vérifiez votre boîte mail")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {

        // TODO: extraire le clientId depuis le JWT (ici simplifié)
        Long clientId = 1L;

        boolean ok = clientService.changePassword(clientId, request.getOldPassword(), request.getNewPassword());
        if (!ok) throw new UnauthorizedAccessException("Ancien mot de passe incorrect");

        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès",
                new MessageResponse("Mot de passe mis à jour")));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestBody String token) {
        boolean isValid = jwtTokenProvider.validateToken(token);
        if (!isValid) {
            return ResponseEntity.ok(ApiResponse.success("Token invalide", false));
        }
        Long clientId = jwtTokenProvider.getClientIdFromToken(token);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Token valide pour " + clientId + " (" + email + ")", true));
    }

    @PostMapping("/decode-token")
    public ResponseEntity<ApiResponse<Object>> decodeToken(@RequestBody String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.ok(ApiResponse.error("Token invalide"));
        }
        return ResponseEntity.ok(ApiResponse.success("Token décodé", Map.of(
                "clientId",   jwtTokenProvider.getClientIdFromToken(token),
                "email",      jwtTokenProvider.getEmailFromToken(token),
                "nomComplet", jwtTokenProvider.getNomCompletFromToken(token),
                "isExpired",  jwtTokenProvider.isTokenExpired(token)
        )));
    }
}
