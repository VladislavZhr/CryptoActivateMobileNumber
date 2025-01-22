package com.example.smsservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SMS_MESSAGES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SMSMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phone_number_id", nullable = false)
    private PhoneNumber phoneNumber;

    private String message;
    private String sender;
    private String status;
    private String dateTime;
}