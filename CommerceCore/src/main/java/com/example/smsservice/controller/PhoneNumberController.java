package com.example.smsservice.controller;

import com.example.smsservice.dto.*;
import com.example.smsservice.model.PhoneNumber;
import com.example.smsservice.service.PhoneNumberService;
import com.example.smsservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phone-numbers")
public class PhoneNumberController {

    private static final Logger logger = LoggerFactory.getLogger(PhoneNumberController.class);
    private final PhoneNumberService phoneNumberService;
    private final UserService userService;

    @Autowired
    public PhoneNumberController(PhoneNumberService phoneNumberService, UserService userService) {
        this.phoneNumberService = phoneNumberService;
        this.userService = userService;
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnPhoneNumber(@RequestBody Map<String, Object> request) {
        try {
            String username = validateAndExtract(request, "username");
            String phoneNumber = validateAndExtract(request, "phoneNumber");

            logger.info("Request to return phone number. Username: {}, PhoneNumber: {}", username, phoneNumber);

            String result = phoneNumberService.returnPhoneNumber(username, phoneNumber);

            return ResponseEntity.ok(Map.of("message", result));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing return request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
        }
    }


    @GetMapping("/available")
    public ResponseEntity<List<PhoneNumber>> getAvailableNumbers(@RequestParam String serviceName) {
        List<PhoneNumber> numbers = phoneNumberService.fetchAvailableNumbers(serviceName);
        return ResponseEntity.ok(numbers);
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchasePhoneNumber(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            String serviceName = (String) request.get("service");
            Double price = Double.parseDouble(request.get("price").toString());
            String rentalType = (String) request.get("rentalType"); // Новый параметр

            if (username == null || serviceName == null || price == null || rentalType == null) {
                throw new IllegalArgumentException("Missing required parameters");
            }

            logger.info("Purchase request received: username={}, service={}, price={}, rentalType={}",
                    username, serviceName, price, rentalType);

            PhoneNumber phoneNumber = phoneNumberService.purchasePhoneNumber(username, serviceName, price, rentalType);

            Map<String, Object> response = Map.of(
                    "phoneNumber", phoneNumber.getPhoneNumber(),
                    "serviceName", phoneNumber.getServiceName(),
                    "price", phoneNumber.getPrice(),
                    "status", phoneNumber.getStatus(),
                    "expires", phoneNumber.getExpires(),
                    "message", "Phone number purchased successfully!"
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing purchase request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    private String validateAndExtract(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || !(value instanceof String) || ((String) value).isBlank()) {
            throw new IllegalArgumentException("Missing or invalid parameter: " + key);
        }
        return (String) value;
    }

    private Double validateAndParseDouble(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid parameter format for: " + key);
        }
    }

    @GetMapping("/by-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PhoneNumber>> getNumbersByOwner(@RequestParam String username) {
        List<PhoneNumber> numbers = phoneNumberService.getNumbersByOwner(username);
        return ResponseEntity.ok(numbers);
    }

    @PutMapping("/transfer")
    @PreAuthorize("hasRole('ADMIN')") // Только администратор
    public ResponseEntity<String> transferNumber(@RequestParam Long numberId,
                                                 @RequestParam String recipientUsername) {
        phoneNumberService.transferNumber(numberId, recipientUsername);
        return ResponseEntity.ok("Phone number transferred successfully.");
    }
}
