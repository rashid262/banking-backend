package com.bank.transaction.controller;

import com.bank.transaction.dto.ApiResponse;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:5173") // To match your React port
public class AdminController {

    private final TransactionService transactionService;

    public AdminController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 🕵️ THE MONEY TRACKER ENDPOINT
    @GetMapping("/transactions/all")
    public ResponseEntity<ApiResponse<List<Transaction>>> getGlobalFlow() {
        try {
            List<Transaction> logs = transactionService.getAllTransactionsForAdmin();
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Global logs retrieved", logs));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>("ERROR", "Failed to fetch logs: " + e.getMessage(), null));
        }
    }
}