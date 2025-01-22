package com.example.smsservice.controller;

import com.example.smsservice.dto.SMSMessageDTO;
import com.example.smsservice.service.PhoneNumberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SMSController {

    private final PhoneNumberService phoneNumberService;

    public SMSController(PhoneNumberService phoneNumberService) {
        this.phoneNumberService = phoneNumberService;
    }

    @PostMapping("/messages")
    public ResponseEntity<List<SMSMessageDTO>> getSMSMessages(@RequestBody Map<String, String> requestBody) {
        String phoneNumber = requestBody.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            List<SMSMessageDTO> smsMessages = phoneNumberService.fetchSMSMessagesForPhoneNumber(phoneNumber);
            return ResponseEntity.ok(smsMessages);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(500).build();
        }
    }
}
