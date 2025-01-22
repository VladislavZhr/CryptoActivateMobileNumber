package com.example.smsservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-email")
public class EmailTestController {

    @Autowired
    private JavaMailSender javaMailSender;

    @GetMapping("/send")
    public String sendTestEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("test-recipient@example.com");
            message.setSubject("Test Email");
            message.setText("This is a test email sent using Gmail SMTP.");

            javaMailSender.send(message);
            return "Email sent successfully!";
        } catch (Exception e) {
            return "Error sending email: " + e.getMessage();
        }
    }
}
