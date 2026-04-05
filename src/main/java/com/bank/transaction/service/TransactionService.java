package com.bank.transaction.service;

import com.bank.transaction.domain.Account;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.domain.User;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.TransactionRepository;
import com.bank.transaction.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Transaction transferMoney(String fromId, String toId, BigDecimal amount, String note, String idempotencyKey) {
        // 1. SECURITY CHECK: Verify the Sender's Identity
        User sourceUser = userRepository.findByAccountId(fromId)
                .orElseThrow(() -> new RuntimeException("Security Error: Sender identity not found."));

        if (!sourceUser.isVerified()) {
            throw new RuntimeException("Transaction Denied: Account not verified. Please check your email: " + sourceUser.getEmail());
        }

        // 2. Fetch Account Details
        Account source = accountRepository.findById(fromId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account destination = accountRepository.findById(toId)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));

        // 3. Balance Check
        if (source.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in your account.");
        }

        // 4. Forex Conversion Logic
        BigDecimal amountToDebit = amount;
        BigDecimal amountToCredit = amount;

        if (!source.getCurrency().equals(destination.getCurrency())) {
            double rate = getExchangeRate(source.getCurrency(), destination.getCurrency());
            amountToCredit = amount.multiply(BigDecimal.valueOf(rate));
        }

        // 5. Update Descriptions for Mini-Statement
        String userNote = (note == null || note.trim().isEmpty()) ? "No note provided" : note;
        source.setDescription("Last Sent: " + userNote);
        destination.setDescription("Last Received: " + userNote);

        // 6. Execute Money Movement
        source.debit(amountToDebit);
        destination.credit(amountToCredit);

        accountRepository.save(source);
        accountRepository.save(destination);

        // 7. Audit Trail
        String sDesc = "Transfer to " + destination.getAccountId() + " (" + userNote + ")";
        String dDesc = "Transfer from " + source.getAccountId() + " (" + userNote + ")";

        return transactionRepository.save(Transaction.builder()
                .sourceAccountId(fromId)
                .destinationAccountId(toId)
                .sourceAmount(amountToDebit)
                .amount(amountToCredit)
                .sourceDescription(sDesc)
                .destinationDescription(dDesc)
                .type("TRANSFER")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build());
    }

    private double getExchangeRate(String from, String to) {
        if (from.equals("USD") && to.equals("INR")) return 83.0;
        if (from.equals("INR") && to.equals("USD")) return 1.0 / 83.0;
        return 1.0;
    }

    public List<Transaction> getTransactionHistory(String accountId) {
        return transactionRepository.findByAnyAccountId(accountId);
    }

    public List<Transaction> getAllTransactionsForAdmin() {
        // This pulls every record from the transactions table
        return transactionRepository.findAllTransactionsGlobal();
    }
}