package com.banking.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    private BigDecimal balance;
    private BigDecimal requestedAmount;

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(BigDecimal balance, BigDecimal requestedAmount) {
        super(String.format("Solde insuffisant. Solde actuel: %s, Montant demandé: %s",
                balance, requestedAmount));
        this.balance = balance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}