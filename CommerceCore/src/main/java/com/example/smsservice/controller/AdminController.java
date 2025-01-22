package com.example.smsservice.controller;

import com.example.smsservice.dto.PhoneNumberDTO;
import com.example.smsservice.dto.UserDTO;
import com.example.smsservice.model.PhoneNumber;
import com.example.smsservice.model.User;
import com.example.smsservice.service.MarkupService;
import com.example.smsservice.service.PhoneNumberService;
import com.example.smsservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final PhoneNumberService phoneNumberService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final MarkupService markupService;



    @Autowired
    public AdminController(UserService userService, PhoneNumberService phoneNumberService, MarkupService markupService) {
        this.userService = userService;
        this.phoneNumberService = phoneNumberService;
        this.markupService = markupService;
    }

    @GetMapping("/markup")
    public ResponseEntity<Double> getMarkup() {
        double currentMarkup = markupService.getCurrentMarkup().getValue();
        return ResponseEntity.ok(currentMarkup);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/numbers-by-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PhoneNumber>> getNumbersByOwner(@RequestParam String username) {
        List<PhoneNumber> phoneNumbers = phoneNumberService.getNumbersByOwner(username);
        return ResponseEntity.ok(phoneNumbers);
    }

    @PutMapping("/transfer-number-by-usernames")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, String>> transferNumberByUsernames(@RequestBody Map<String, String> payload) {
        String donorUsername = payload.get("donorUsername");
        String recipientUsername = payload.get("recipientUsername");
        String phoneNumber = payload.get("phoneNumber");

        if (donorUsername == null || recipientUsername == null || phoneNumber == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "All fields (donorUsername, recipientUsername, phoneNumber) are required.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            phoneNumberService.transferNumberByUsername(donorUsername, recipientUsername, phoneNumber);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Phone number transferred successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/markup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> setMarkup(@RequestBody Map<String, Double> request) {
        Double markup = request.get("markup");
        if (markup == null || markup < 0) {
            logger.warn("Invalid markup value: {}", markup);
            return ResponseEntity.badRequest().body("Invalid markup value");
        }

        markupService.updateMarkup(markup);
        return ResponseEntity.ok("Markup updated successfully");
    }




    @PutMapping("/update-balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBalance(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Object newBalanceObj = payload.get("newBalance");

        // Проверка наличия данных
        if (username == null || newBalanceObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both username and newBalance are required."));
        }

        Double newBalance;
        try {
            if (newBalanceObj instanceof Integer) {
                newBalance = ((Integer) newBalanceObj).doubleValue();
            } else if (newBalanceObj instanceof Double) {
                newBalance = (Double) newBalanceObj;
            } else {
                throw new ClassCastException("newBalance must be a numeric value.");
            }
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        try {

            userService.updateUserBalance(username, newBalance);
            return ResponseEntity.ok(Map.of("message", "Balance updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users-with-numbers")
    @PreAuthorize("hasRole('ADMIN')") // Только для администратора
    public ResponseEntity<List<UserDTO>> getUsersWithNumbers() {
        List<User> users = userService.getAllUsers();

        // Преобразование в DTO
        List<UserDTO> userDTOs = users.stream()
                .map(user -> new UserDTO(
                        user.getUsername(),
                        user.getEmail(),
                        user.getBalance(),
                        user.getPhoneNumbers().stream()
                                .map(phone -> new PhoneNumberDTO(
                                        null,
                                        phone.getPhoneNumber(),
                                        phone.getServiceName(),
                                        phone.getPrice(),
                                        phone.getStatus()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDTOs);
    }

    @PutMapping("/transfer-number")
    public ResponseEntity<Map<String, String>> transferNumber(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String recipientUsername = request.get("recipientUsername");

        if (phoneNumber == null || recipientUsername == null) {
            Map<String, String> errorResponse = Map.of("error", "Phone number and recipient username are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            phoneNumberService.transferPhoneNumber(phoneNumber, recipientUsername);
            Map<String, String> successResponse = Map.of("message", "Phone number transferred successfully");
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = Map.of("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @DeleteMapping("/delete-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@RequestParam String username) {
        try {
            System.out.println("Attempting to delete user: " + username);
            userService.deleteUserByUsername(username);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            System.err.println("Error during user deletion: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error during user deletion: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error", "details", e.getMessage()));
        }
    }
}
