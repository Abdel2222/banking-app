package com.banking.controllers;

import com.banking.dto.request.*;
import com.banking.dto.response.*;
import com.banking.entities.Client;
import com.banking.entities.CompteBancaire;
import com.banking.services.ClientService;
import com.banking.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody ClientRegistrationRequest request) {

        Client client = clientService.createClient(
                request.getPrenom(),
                request.getNom(),
                request.getEmail(),
                request.getMotDePasse()
        );

        ClientResponse response = new ClientResponse(client);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client créé avec succès", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getAllClients() {
        List<ClientResponse> clients = clientService.findAll().stream()
                .map(ClientResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable Long id) {
        Client client = clientService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        return ResponseEntity.ok(ApiResponse.success(new ClientResponse(client)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientByEmail(@PathVariable String email) {
        Client client = clientService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "email", email));

        return ResponseEntity.ok(ApiResponse.success(new ClientResponse(client)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {

        Client clientDetails = new Client();
        clientDetails.setPrenom(request.getPrenom());
        clientDetails.setNom(request.getNom());
        clientDetails.setEmail(request.getEmail());

        Client updatedClient = clientService.updateClient(id, clientDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Client mis à jour avec succès", new ClientResponse(updatedClient))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.success("Client supprimé avec succès", null));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {

        boolean success = clientService.changePassword(
                id,
                request.getOldPassword(),
                request.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok(
                    ApiResponse.success("Mot de passe changé avec succès",
                            new MessageResponse("Mot de passe mis à jour"))
            );
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ancien mot de passe incorrect"));
        }
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getClientAccounts(@PathVariable Long id) {
        List<AccountResponse> accounts = clientService.getClientAccounts(id).stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}/accounts/active")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getActiveAccounts(@PathVariable Long id) {
        List<AccountResponse> accounts = clientService.getActiveAccounts(id).stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalBalance(@PathVariable Long id) {
        BigDecimal balance = clientService.getTotalBalance(id);
        return ResponseEntity.ok(ApiResponse.success("Solde total récupéré", balance));
    }

    @PostMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request) {

        CompteBancaire compte = clientService.openAccount(id, request.getTypeCompte());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Compte créé avec succès", new AccountResponse(compte)));
    }

    @GetMapping("/{id}/loan-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkLoanEligibility(@PathVariable Long id) {
        boolean eligible = clientService.isEligibleForLoan(id);
        String message = eligible ? "Client éligible au prêt" : "Client non éligible au prêt";
        return ResponseEntity.ok(ApiResponse.success(message, eligible));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClientResponse>>> searchClients(@RequestParam String name) {
        List<ClientResponse> clients = clientService.searchByName(name).stream()
                .map(ClientResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(clients));
    }
}