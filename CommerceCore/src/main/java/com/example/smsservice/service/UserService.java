package com.example.smsservice.service;

import com.example.smsservice.model.PasswordResetToken;
import com.example.smsservice.model.Role;
import com.example.smsservice.model.User;
import com.example.smsservice.repository.PasswordResetTokenRepository;
import com.example.smsservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService  {

    private final UserRepository userRepository;
    @Qualifier("customPasswordEncoder")
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            System.out.println("Token not found in the database.");
            return false;
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            System.out.println("Token has expired.");
            return false;
        }

        System.out.println("Token is valid.");
        return true;
    }

    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        PasswordResetToken existingToken = passwordResetTokenRepository.findByUser(user);
        if (existingToken != null) {
            existingToken.setToken(UUID.randomUUID().toString());
            existingToken.setExpiryDate(Instant.now().plusSeconds(3600)); // 1 час
            passwordResetTokenRepository.save(existingToken);
            return existingToken.getToken();
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600)) // 1 час
                .build();
        passwordResetTokenRepository.save(passwordResetToken);

        return token;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUserByUsername(String username) {
        System.out.println("Looking for user with username: " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        System.out.println("User found, deleting: " + user.getUsername());
        userRepository.delete(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    public void updateUserBalance(String username, double newBalance) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        user.setBalance(newBalance);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

}