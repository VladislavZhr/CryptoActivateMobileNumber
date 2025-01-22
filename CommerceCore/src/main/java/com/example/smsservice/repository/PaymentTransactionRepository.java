package com.example.smsservice.repository;

import com.example.smsservice.model.PaymentTransaction;
import com.example.smsservice.model.TransactionStatus;
import com.example.smsservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    PaymentTransaction findByTransactionId(String transactionId);
    Optional<PaymentTransaction> findByOrderId(String orderId);
    List<PaymentTransaction> findAllByUser(User user);
}
