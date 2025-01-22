package com.example.smsservice.dto;

import lombok.Data;

import java.util.List;
@Data

public class SimplifiedResponseDTO {
    private List<PhoneNumberDTO> phoneNumbers;
    private String message;
    private boolean success;

    public SimplifiedResponseDTO(List<PhoneNumberDTO> phoneNumbers, String message, boolean success) {
        this.phoneNumbers = phoneNumbers;
        this.message = message;
        this.success = success;
    }

    // Getters and Setters
}