package com.bank.transaction.controller;

import com.bank.transaction.domain.Transaction;
import com.bank.transaction.dto.ApiResponse;
import com.bank.transaction.dto.CreateTransactionRequest;
import com.bank.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transferMoney(@RequestBody CreateTransactionRequest request) {
        Transaction tx = transactionService.transferMoney(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getNote(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Transfer completed!", tx));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistory(@PathVariable String accountId) {
        List<Transaction> history = transactionService.getTransactionHistory(accountId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "History found", history));
    }
}