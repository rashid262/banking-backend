package com.bank.transaction.service;

import com.bank.transaction.domain.Account;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.domain.User;
import com.bank.transaction.enums.Role;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.TransactionRepository;
import com.bank.transaction.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private static final String ACCOUNT_FORMAT = "^UB-\\d{4}$";

    // 🛡️ Password Generation Constants
    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          JavaMailSender mailSender) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    @Transactional
    public Transaction creditAccount(String accountId, BigDecimal amount, String currency, String idempotencyKey) {
        Account account = getAccount(accountId);
        account.credit(amount);
        accountRepository.save(account);

        return transactionRepository.save(Transaction.builder()
                .sourceAccountId("SYSTEM_VAULT")
                .destinationAccountId(accountId)
                .amount(amount)
                .type("CREDIT")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .sourceDescription("Administrative Disbursement")
                .destinationDescription("System Credit")
                .build());
    }

    @Transactional
    public Account createNewUserAccount(String accountId, String username, String email, String currency) {
        // Validation Checks
        if (!accountId.matches(ACCOUNT_FORMAT)) {
            throw new RuntimeException("Format Error: Use UB-XXXX");
        }
        if (accountRepository.existsById(accountId)) {
            throw new RuntimeException("Account ID " + accountId + " already exists.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email " + email + " is already registered.");
        }

        // 1. Generate Secure Random Password for Admin-created user
        String tempPassword = generateRandomPassword();

        // 2. Create Bank Account
        Account account = new Account(accountId, BigDecimal.ZERO, currency, true);
        account.setDescription("Account provisioned by Administrator");
        accountRepository.save(account);

        // 3. Create User with verification token and encoded temp password
        String vToken = UUID.randomUUID().toString();
        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .accountId(accountId)
                .role(Role.CUSTOMER)
                .isVerified(false)
                .verificationToken(vToken)
                .build();
        userRepository.save(newUser);

        // 4. Send email containing BOTH the link and the password
        sendProvisioningEmail(email, vToken, tempPassword);

        return account;
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    private void sendProvisioningEmail(String email, String token, String tempPass) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Universal Bank - New Account Created");

            String content = "Welcome to Universal Bank!\n\n" +
                    "An administrator has created an account for you.\n" +
                    "Your Temporary Password: " + tempPass + "\n\n" +
                    "Please click the link below to verify your account and enable login:\n" +
                    "http://localhost:8080/api/v1/auth/verify?token=" + token + "\n\n" +
                    "Note: For security, change your password immediately after your first login.";

            message.setText(content);
            mailSender.send(message);
            System.out.println("📧 Provisioning email sent to: " + email);
        } catch (Exception e) {
            System.err.println("❌ Mail Error: " + e.getMessage());
            // Log the password to console in Dev mode if email fails so you can still test
            System.out.println("⚠️ EMAIL FAILED. Dev Temp Password for " + email + " is: " + tempPass);
        }
    }
}