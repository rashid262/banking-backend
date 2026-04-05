package com.bank.transaction.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceAccountId;
    private String destinationAccountId;

    private BigDecimal amount;
    private BigDecimal sourceAmount; // Required for Forex logic

    private String type; // CREDIT, TRANSFER, DEBIT
    private String status;
    private String idempotencyKey;
    private LocalDateTime createdAt;

    private String sourceDescription;
    private String destinationDescription;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    public void markSuccess() {
        this.status = "SUCCESS";
    }
}