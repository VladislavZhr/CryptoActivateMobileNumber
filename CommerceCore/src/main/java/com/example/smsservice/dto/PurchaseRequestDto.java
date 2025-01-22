package com.example.smsservice.dto;

import lombok.Data;

@Data
public class PurchaseRequestDto {
    private String username;
    private String serviceName;
    private Double price;
}
