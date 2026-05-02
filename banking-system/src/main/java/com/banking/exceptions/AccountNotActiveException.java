package com.banking.exceptions;

public class AccountNotActiveException extends RuntimeException {
    private String accountNumber;

    public AccountNotActiveException(String message) {
        super(message);
    }

    public AccountNotActiveException(String accountNumber, String status) {
        super(String.format("Le compte %s n'est pas actif. Statut actuel: %s",
                accountNumber, status));
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}