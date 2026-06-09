package com.kilgore.vaultiq.shared.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient wallet balance");
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance: requested %s but only %s available", requested, available));
    }
}
