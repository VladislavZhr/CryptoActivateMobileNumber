package com.example.smsservice.dto;

import lombok.Data;

@Data
public class PasswordUpdateRequest {
    private String email;
    private String newPassword;
    private String token;
}
