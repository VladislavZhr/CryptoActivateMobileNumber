package com.example.smsservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/services")
    public ResponseEntity<Object> fetchLongTermServices() {
        String externalApiUrl = "";

        try {
            logger.info("Fetching long-term services from external API: {}", externalApiUrl);

            Object response = restTemplate.getForObject(externalApiUrl, Object.class);

            logger.info("Long-term services response received successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error while fetching long-term services from external API", e);
            return ResponseEntity.internalServerError().body("Failed to fetch long-term services. Please try again later.");
        }
    }

    @GetMapping("/short-term-services")
    public ResponseEntity<Object> fetchShortTermServices() {
        String externalApiUrl = "";

        try {
            logger.info("Fetching short-term services from external API: {}", externalApiUrl);
            Object response = restTemplate.getForObject(externalApiUrl, Object.class);
            logger.info("Short-term services response received successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error while fetching short-term services from external API", e);
            return ResponseEntity.internalServerError().body("Failed to fetch short-term services. Please try again later.");
        }
    }
}