package com.bank.transaction.controller;

import com.bank.transaction.dto.ApiResponse;
import com.bank.transaction.dto.AuthRequest;
import com.bank.transaction.dto.AuthResponse;
import com.bank.transaction.domain.User;
import com.bank.transaction.dto.ChangePasswordRequest;
import com.bank.transaction.service.AuthService;
import com.bank.transaction.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody User user) {
        String message = authService.register(user);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", message, null));
    }

    // New Endpoint for Email Verification
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestParam String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        // Redirect to your React frontend success page
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("http://localhost:5173/verify-success"))
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(
                    request.getUsername(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );
            // Return structured JSON so frontend can parse it
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Password updated successfully.", null));
        } catch (Exception e) {
            // Return structured Error JSON
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
}