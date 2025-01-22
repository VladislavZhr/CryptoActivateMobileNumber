package com.example.smsservice.service;

import com.example.smsservice.dto.TransactionDTO;
import com.example.smsservice.model.PaymentTransaction;
import com.example.smsservice.model.TransactionStatus;
import com.example.smsservice.model.User;
import com.example.smsservice.repository.PaymentTransactionRepository;
import com.example.smsservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate;

    @Value("${nowpayments.api.key}")
    private String apiKey;

    @Value("${nowpayments.ipn.secret}")
    private String ipnSecret;

    private static final String NOWPAYMENTS_BASE_URL = "https://api.nowpayments.io/v1";
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(UserRepository userRepository, RestTemplate restTemplate,
                          PaymentTransactionRepository paymentTransactionRepository) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public List<TransactionDTO> getTransactionsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PaymentTransaction> transactions = paymentTransactionRepository.findAllByUser(user);
        return transactions.stream().map(this::toTransactionDTO).collect(Collectors.toList());
    }


    private TransactionDTO toTransactionDTO(PaymentTransaction transaction) {
        return new TransactionDTO(
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getUser().getUsername()
        );
    }

    @Transactional
    public void updateBalance(String username, double amount, boolean isAdding) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        double newBalance = isAdding ? user.getBalance() + amount : user.getBalance() - amount;
        if (newBalance < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        user.setBalance(newBalance);
        userRepository.save(user);
    }

    public List<TransactionDTO> getAllTransactionsExcludingPending() {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll();
        return transactions.stream()
                .filter(transaction -> transaction.getStatus() != TransactionStatus.PENDING)
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    private TransactionStatus mapStatus(String paymentStatus) {
        switch (paymentStatus.toLowerCase()) {
            case "success":
                return TransactionStatus.FINISHED;
            case "failed":
                return TransactionStatus.FAILED;
            case "waiting":
                return TransactionStatus.PENDING; // Мапим на PENDING
            default:
                throw new IllegalArgumentException("Unknown payment status: " + paymentStatus);
        }
    }

    @Transactional
    public void processCallback(String orderId, String transactionId, String paymentStatus, double amountPaid) {
        logger.info("Processing callback: orderId={}, transactionId={}, status={}, amountPaid={}",
                orderId, transactionId, paymentStatus, amountPaid);

        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for orderId: " + orderId));

        TransactionStatus status = mapStatus(paymentStatus);

        // Если статус PENDING, обновляем только статус
        if (status == TransactionStatus.PENDING) {
            transaction.setStatus(status);
            paymentTransactionRepository.save(transaction);
            logger.info("Transaction updated to PENDING for orderId={}", orderId);
            return;
        }

        // Если статус FINISHED, обновляем баланс
        if (status == TransactionStatus.FINISHED) {
            updateBalance(transaction.getUser().getUsername(), amountPaid, true);
            logger.info("Transaction completed successfully for user: {}", transaction.getUser().getUsername());
        }

        // Обновляем статус и сохраняем
        transaction.setStatus(status);
        if (transactionId != null) {
            transaction.setTransactionId(transactionId);
        }
        paymentTransactionRepository.save(transaction);
    }


    public double getUserBalance(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getBalance();
    }

    public String createPayment(String orderId, double amount, String currency, String crypto, String username) {
        logger.info("Creating payment transaction for orderId: {}", orderId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setCrypto(crypto);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setUser(user);

        try {
            paymentTransactionRepository.save(transaction);
            logger.info("Transaction saved successfully: {}", transaction);

            Map<String, Object> requestBody = Map.of(
                    "price_amount", amount,
                    "price_currency", currency,
                    "pay_currency", crypto,
                    "order_id", orderId,
                    "order_description", "Payment for order " + orderId
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(NOWPAYMENTS_BASE_URL + "/invoice", requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Invoice created successfully for orderId: {}", orderId);
                return extractField(response.getBody(), "invoice_url");
            } else {
                throw new RuntimeException("Failed to create payment: " + response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error creating payment for orderId: {}", orderId, e);
            throw new RuntimeException("Error creating payment: " + e.getMessage(), e);
        }
    }

    public String checkPaymentStatus(String paymentId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                NOWPAYMENTS_BASE_URL + "/payment/" + paymentId, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return extractField(response.getBody(), "payment_status");
        } else {
            throw new RuntimeException("Failed to fetch payment status: " + response.getBody());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String extractField(String responseBody, String fieldName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get(fieldName).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response field: " + fieldName, e);
        }
    }
}
