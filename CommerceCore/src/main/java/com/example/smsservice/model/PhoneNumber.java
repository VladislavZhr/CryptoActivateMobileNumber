package com.example.smsservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PHONE_NUMBERS")
public class PhoneNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private String serviceName;
    private Double price;
    private String status;
    private String externalId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "phoneNumber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SMSMessage> messages = new ArrayList<>();

    @Column(name = "expires")
    private LocalDateTime expires;

    public PhoneNumber(String phoneNumber, String serviceName, Double price, String status) {
        this.phoneNumber = phoneNumber;
        this.serviceName = serviceName;
        this.price = price;
        this.status = status;
    }
}
