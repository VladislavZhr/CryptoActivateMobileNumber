
package com.example.smsservice.controller;

import com.example.smsservice.dto.PasswordUpdateRequest;
import com.example.smsservice.dto.PhoneNumberDTO;
import com.example.smsservice.dto.SuccessResponse;
import com.example.smsservice.model.ErrorResponse;
import com.example.smsservice.model.LoginRequest;
import com.example.smsservice.model.PhoneNumber;
import com.example.smsservice.model.User;
import com.example.smsservice.repository.UserRepository;
import com.example.smsservice.service.EmailService;
import com.example.smsservice.service.PhoneNumberService;
import com.example.smsservice.service.UserService;
import com.example.smsservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "https://www.realsimus.online")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PhoneNumberService phoneNumberService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authenticationManager, PhoneNumberService phoneNumberService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.phoneNumberService = phoneNumberService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/password-update")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateRequest request) {
        try {
            System.out.println("Password update request received for email: " + request.getEmail());

            if (request.getToken() == null || request.getToken().isEmpty()) {
                System.out.println("Token is missing");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token is missing"));
            }

            boolean isTokenValid = userService.validatePasswordResetToken(request.getToken());
            if (!isTokenValid) {
                System.out.println("Invalid or expired token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or expired token"));
            }

            userService.updatePassword(request.getEmail(), request.getNewPassword());
            System.out.println("Password updated successfully for email: " + request.getEmail());
            return ResponseEntity.ok(new SuccessResponse("Password updated successfully"));
        } catch (RuntimeException e) {
            System.out.println("Error during password update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("User not found", e.getMessage()));
        }
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        System.out.println("Received forgot-password request for email: " + email);

        if (email == null || email.isEmpty()) {
            System.out.println("Email is missing in request");
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        try {
            String resetToken = userService.generatePasswordResetToken(email);
            System.out.println("Generated reset token: " + resetToken);

            String resetLink = "https://1develop1.wixstudio.com/realsimus/reset?token=" + resetToken;
            System.out.println("Reset link: " + resetLink);

            emailService.sendResetPasswordEmail(email, resetLink);
            System.out.println("Reset email sent successfully to: " + email);

            return ResponseEntity.ok(Map.of("message", "Password reset email sent successfully"));
        } catch (RuntimeException e) {
            System.err.println("Error during password reset process: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", registeredUser.getUsername());
            response.put("email", registeredUser.getEmail());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");

            String token = jwtUtil.generateToken(loginRequest.getUsername(), role);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", loginRequest.getUsername(),
                    "role", role
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @GetMapping("/phone-numbers")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PhoneNumberDTO>> getUserPhoneNumbers(Authentication authentication) {
        String username = authentication.getName();
        List<PhoneNumberDTO> phoneNumbers = phoneNumberService.getPhoneNumbersByUsername(username);
        return ResponseEntity.ok(phoneNumbers);
    }

    @GetMapping("/my-phone-numbers")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PhoneNumberDTO>> getUserPhoneNumbers(Principal principal) {
        // Получить имя пользователя из Principal
        String username = principal.getName();
        User user = userService.findByUsername(username);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<PhoneNumber> phoneNumbers = phoneNumberService.getPhoneNumbersByUserId(user.getId());

        List<PhoneNumberDTO> phoneNumberDTOs = phoneNumbers.stream()
                .map(phone -> new PhoneNumberDTO(
                        null,
                        phone.getPhoneNumber(),
                        phone.getServiceName(),
                        null,
                        phone.getStatus()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(phoneNumberDTOs);
    }


    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile() {
        try {

            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = userService.findByUsername(username);

            Map<String, Object> userData = Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "balance", user.getBalance(),
                    "password", "******"
            );

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch user data"));
        }
    }

    @PatchMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest) {
        try {

            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");

            userService.changePassword(username, oldPassword, newPassword);

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}