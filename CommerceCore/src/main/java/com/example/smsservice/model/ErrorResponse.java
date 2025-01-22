package com.example.smsservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String details;

    public ErrorResponse(String error) {
        this.error = error;
        this.details = null;
    }
}
