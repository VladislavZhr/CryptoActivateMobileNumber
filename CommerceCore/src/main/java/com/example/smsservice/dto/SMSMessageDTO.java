package com.example.smsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SMSMessageDTO {
    private String date;
    private String sender;
    private String message;
}
