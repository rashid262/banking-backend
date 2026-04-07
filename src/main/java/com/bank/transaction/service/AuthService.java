package com.bank.transaction.service;

import com.bank.transaction.domain.Account;
import com.bank.transaction.domain.User;
import com.bank.transaction.dto.AuthRequest;
import com.bank.transaction.dto.AuthResponse;
import com.bank.transaction.enums.Role;
import com.bank.transaction.repository.AccountRepository;
import com.bank.transaction.repository.UserRepository;
import com.bank.transaction.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.verification.url}")
    private String verificationBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email address before logging in.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getAccountId(), user.getRole().name());
    }

    @Transactional
    public String register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }

        prepareAccountAndUser(user, user.getPasswordHash(), false);

        String vToken = UUID.randomUUID().toString();
        user.setVerificationToken(vToken);
        userRepository.save(user);

        sendEmail(user.getEmail(), vToken, null);
        return "Registration successful! Check your email to verify.";
    }

    @Transactional
    public String provisionByAdmin(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Account already exists with this email.");
        }

        String randomPass = generateRandomPassword();
        prepareAccountAndUser(user, randomPass, true);

        String vToken = UUID.randomUUID().toString();
        user.setVerificationToken(vToken);
        userRepository.save(user);

        sendEmail(user.getEmail(), vToken, randomPass);
        return "Account provisioned. Secure password sent to " + user.getEmail();
    }

    private void prepareAccountAndUser(User user, String rawPassword, boolean isAdminCreated) {
        Account newAccount = new Account(user.getAccountId(), BigDecimal.ZERO,
                user.getCurrency() != null ? user.getCurrency() : "INR", true);
        newAccount.setDescription(isAdminCreated ? "Created by System Admin" : "Self-registered account");
        accountRepository.save(newAccount);

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.CUSTOMER);
        user.setVerified(false);
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    public void sendEmail(String email, String token, String tempPass) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Universal Bank - Verify Your Account");

            StringBuilder body = new StringBuilder("Welcome to Universal Bank!\n\n");
            if (tempPass != null) {
                body.append("An account was created for you by an administrator.\n");
                body.append("Your Temporary Password is: ").append(tempPass).append("\n\n");
            }
            body.append("Please verify your account by clicking the link below:\n");
            body.append(verificationBaseUrl).append(token);

            message.setText(body.toString());
            mailSender.send(message);
            System.out.println("✅ Email successfully sent to " + email);
        } catch (Exception e) {
            // Log full error for Render troubleshooting
            System.err.println("❌ Mail Delivery Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("The current password you entered is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}