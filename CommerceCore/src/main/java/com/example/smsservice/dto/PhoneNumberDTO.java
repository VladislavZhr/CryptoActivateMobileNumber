package com.example.smsservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhoneNumberDTO {
    private Long id;
    private String phoneNumber;
    private String serviceName;
    private Double price;
    private String status;
}
