package com.bank.transaction.controller;

import com.bank.transaction.domain.Account;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.dto.ApiResponse;
import com.bank.transaction.dto.CreateTransactionRequest;
import com.bank.transaction.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "http://localhost:5173")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(@RequestBody Map<String, String> request) {
        Account newAccount = accountService.createNewUserAccount(
                request.get("accountId"),
                request.get("username"),
                request.get("email"), // 👈 Extracting email
                request.get("currency")
        );

        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Account " + newAccount.getAccountId() + " provisioned successfully",
                newAccount
        ));
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<ApiResponse<Transaction>> creditAccount(
            @PathVariable String accountId,
            @RequestBody CreateTransactionRequest request
    ) {
        Transaction transaction = accountService.creditAccount(
                accountId,
                request.getAmount(),
                request.getCurrency(),
                request.getIdempotencyKey()
        );

        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Funds dispatched", transaction));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<Account>> getBalance(@PathVariable String accountId) {
        Account account = accountService.getAccount(accountId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Balance retrieved", account));
    }
}