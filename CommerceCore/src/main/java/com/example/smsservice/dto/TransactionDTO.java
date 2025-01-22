package com.example.smsservice.dto;

import com.example.smsservice.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private String transactionId;
    private double amount;
    private TransactionStatus status;
    private LocalDateTime date;
    private String username;
}
