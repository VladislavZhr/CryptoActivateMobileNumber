package com.example.smsservice.controller;

import com.example.smsservice.dto.TransactionDTO;
import com.example.smsservice.model.PaymentTransaction;
import com.example.smsservice.model.TransactionStatus;
import com.example.smsservice.model.User;
import com.example.smsservice.repository.PaymentTransactionRepository;
import com.example.smsservice.repository.UserRepository;
import com.example.smsservice.service.PaymentService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import org.antlr.v4.runtime.misc.LogManager;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${nowpayments.api.key}")
    private String apiKey;

    @Value("${nowpayments.ipn.secret}")
    private String ipnSecret;

    private final PaymentService paymentService;
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;


    public PaymentController(PaymentService paymentService, UserRepository userRepository, PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentService = paymentService;
        this.objectMapper = new ObjectMapper();
        this.userRepository = userRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }


    @PostMapping("/update-balance")
    public ResponseEntity<?> updateBalance(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            double amount = Double.parseDouble(request.get("amount").toString());
            boolean isAdding = Boolean.parseBoolean(request.get("isAdding").toString());

            paymentService.updateBalance(username, amount, isAdding);
            String action = isAdding ? "added to" : "deducted from";
            return ResponseEntity.ok(Map.of("message", "Balance " + action + " user successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@RequestParam String username) {
        try {
            List<TransactionDTO> transactions = paymentService.getTransactionsByUsername(username);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/get-balance")
    public ResponseEntity<?> getBalance(@RequestParam String username) {
        try {
            double balance = paymentService.getUserBalance(username);
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all-transactions")
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        try {
            List<TransactionDTO> transactions = paymentService.getAllTransactionsExcludingPending();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            logger.error("Error fetching all transactions", e); // Логируем ошибку
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/callback")
    public ResponseEntity<String> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        try {
            logger.info("Received callback payload: {}", payload);

            // Validate payload
            if (!validateCallbackPayload(payload)) {
                logger.warn("Invalid callback payload: {}", payload);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid callback payload");
            }

            String orderId = payload.get("order_id").toString();
            String paymentStatus = payload.get("payment_status").toString();
            String transactionId = payload.get("payment_id") != null ? payload.get("payment_id").toString() : null;
            double amountPaid = payload.containsKey("actually_paid") ? Double.parseDouble(payload.get("actually_paid").toString()) : 0;

            // Delegate logic to the service layer
            paymentService.processCallback(orderId, transactionId, paymentStatus, amountPaid);

            return ResponseEntity.ok("Callback processed successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid callback data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing payment callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing callback");
        }
    }

    // Validate payload
    private boolean validateCallbackPayload(Map<String, Object> payload) {
        return payload.containsKey("order_id") &&
                payload.containsKey("payment_status") &&
                !payload.get("order_id").toString().isEmpty() &&
                !payload.get("payment_status").toString().isEmpty();
    }




    /**
     * Создание платежа.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> paymentRequest) {
        try {
            String orderId = (String) paymentRequest.get("orderId");
            double amount = Double.parseDouble(paymentRequest.get("amount").toString());
            String currency = (String) paymentRequest.get("currency");
            String crypto = (String) paymentRequest.get("crypto");
            String username = (String) paymentRequest.get("username");

            String paymentUrl = paymentService.createPayment(orderId, amount, currency, crypto, username);
            return ResponseEntity.ok(Map.of("payment_url", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Проверка статуса платежа.
     */
    @PostMapping("/status")
    public ResponseEntity<?> checkPaymentStatus(@RequestBody Map<String, String> statusRequest) {
        try {
            String paymentId = statusRequest.get("paymentId");
            String status = paymentService.checkPaymentStatus(paymentId);

            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
