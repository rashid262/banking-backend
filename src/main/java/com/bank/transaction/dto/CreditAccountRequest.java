package com.bank.transaction.dto;
import java.math.BigDecimal;

public class CreditAccountRequest {
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;

    // Getters and Setters
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
}