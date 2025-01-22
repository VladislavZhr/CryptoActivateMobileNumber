package com.example.smsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponseDto {
    private String phoneNumber;
    private String status;
    private Double price;
    private String serviceName;
    private String message;
}
