package com.example.smsservice.service;

import com.example.smsservice.dto.PhoneNumberDTO;
import com.example.smsservice.dto.SMSMessageDTO;
import com.example.smsservice.model.*;
import com.example.smsservice.repository.PhoneNumberRepository;
import com.example.smsservice.repository.SMSMessageRepository;
import com.example.smsservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PhoneNumberService {

    private static final Logger logger = LoggerFactory.getLogger(PhoneNumberService.class);

    private final PhoneNumberRepository phoneNumberRepository;
    private final UserRepository userRepository;
    private final SMSMessageRepository smsMessageRepository;

    private final RestTemplate restTemplate;

    public PhoneNumberService(
            PhoneNumberRepository phoneNumberRepository,
            UserRepository userRepository,
            SMSMessageRepository smsMessageRepository
    ) {
        this.phoneNumberRepository = phoneNumberRepository;
        this.userRepository = userRepository;
        this.smsMessageRepository = smsMessageRepository;

        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public List<SMSMessageDTO> fetchSMSMessagesForPhoneNumber(String phoneNumber) {
        PhoneNumber phoneNumberEntity = phoneNumberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Phone number not found in the database"));

        String apiUrl = "";
        String fullUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("cmd", "")
                .queryParam("user", "")
                .queryParam("api_key", "")
                .queryParam("mdn", phoneNumber)
                .toUriString();

        System.out.println("Fetching SMS messages from API: " + fullUrl);

        ResponseEntity<Map> response = restTemplate.getForEntity(fullUrl, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to fetch SMS messages from API. HTTP Status: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"ok".equals(responseBody.get("status"))) {
            // Обработка "No messages"
            if ("No messages".equals(responseBody.get("message"))) {
                System.out.println("No SMS messages found for phone number: " + phoneNumber);
                return Collections.emptyList();
            }
            throw new RuntimeException("Error from API: " + responseBody.get("message"));
        }

        List<Map<String, Object>> messages = (List<Map<String, Object>>) responseBody.get("message");
        if (messages == null || messages.isEmpty()) {
            System.out.println("No SMS messages found for the provided phone number.");
            return Collections.emptyList();
        }

        return messages.stream().map(message -> {
            SMSMessageDTO smsDTO = new SMSMessageDTO();
            smsDTO.setDate((String) message.get("date_time"));
            smsDTO.setSender((String) message.get("from"));
            smsDTO.setMessage((String) message.get("reply"));
            return smsDTO;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> callApi(String cmd, Map<String, String> params) {
        String apiUrl = "";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("cmd", cmd)
                .queryParam("user", "")
                .queryParam("api_key", "");

        params.forEach(uriBuilder::queryParam);

        String requestUrl = uriBuilder.toUriString();

        logger.info("Sending API request to command '{}'. Endpoint invoked.", cmd);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !"ok".equalsIgnoreCase((String) responseBody.get("status"))) {
                String errorMessage = responseBody != null
                        ? (String) responseBody.getOrDefault("message", "Unknown error")
                        : "No response body";
                throw new ApiException("API error for command '" + cmd + "': " + errorMessage);
            }
            return responseBody;
        } catch (Exception e) {
            throw new ApiException("Failed to call API command '" + cmd + "': " + e.getMessage(), e);
        }
    }

    @Transactional
    public String returnPhoneNumber(String username, String phoneNumber) {
        logger.info("Returning phone number for user: {}, phoneNumber: {}", username, phoneNumber);

        PhoneNumber phone = phoneNumberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new DatabaseException("Phone number not found in the database"));

        logger.info("Phone number found: {}, price: {}", phone.getPhoneNumber(), phone.getPrice());

        if (phone.getPrice() < 5) {
            return rejectTemporaryNumber(phone, username);
        } else {
            return releaseLongTermNumber(phone, username);
        }
    }

    private String rejectTemporaryNumber(PhoneNumber phone, String username) {
        logger.info("Rejecting temporary phone number. External ID: {}", phone.getExternalId());

        Map<String, String> params = Map.of("id", phone.getExternalId());
        callApi("reject", params);

        logger.info("Temporary phone number rejected successfully. Initiating refund...");
        refundUser(username, phone.getPrice());

        // Удаление номера из базы данных
        phoneNumberRepository.delete(phone);
        logger.info("Temporary phone number deleted from database: {}", phone.getPhoneNumber());

        return "Temporary phone number rejected successfully, funds refunded, and number deleted.";
    }

    private String releaseLongTermNumber(PhoneNumber phone, String username) {
        logger.info("Releasing long-term phone number: {}", phone.getPhoneNumber());

        Map<String, String> params = Map.of(
                "mdn", phone.getPhoneNumber(),
                "service", phone.getServiceName()
        );
        callApi("ltr_release", params);

        logger.info("Long-term phone number released successfully. Initiating refund...");
        refundUser(username, phone.getPrice());

        phoneNumberRepository.delete(phone);
        logger.info("Long-term phone number deleted from database: {}", phone.getPhoneNumber());

        return "Long-term phone number released successfully, funds refunded, and number deleted.";
    }

    @Transactional
    public void refundUser(String username, Double amount) {
        if (amount <= 0) {
            logger.warn("Attempt to refund non-positive amount: {}", amount);
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DatabaseException("User not found"));

        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);

        logger.info("User {} refunded successfully. Refund amount: {}, New balance: {}", username, amount, user.getBalance());
    }

    @Transactional
    public PhoneNumber purchasePhoneNumber(String username, String serviceName, Double price, String rentalType) {
        logger.info("Starting purchase process for user: {}, service: {}, price: {}, rentalType: {}",
                username, serviceName, price, rentalType);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        logger.info("User found: {}", user);

        if (user.getBalance() < price) {
            logger.error("Insufficient balance for user: {}", username);
            throw new RuntimeException("Insufficient balance for user: " + username);
        }

        String apiUrl = "";
        UriComponentsBuilder uriBuilder;

        if ("long_term".equalsIgnoreCase(rentalType)) {

            uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("cmd", "")
                    .queryParam("user", "")
                    .queryParam("api_key", "")
                    .queryParam("service", serviceName)
                    .queryParam("duration", 30);
        } else {

            uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("cmd", "")
                    .queryParam("user", "")
                    .queryParam("api_key", "")
                    .queryParam("service", serviceName);
        }

        logger.info("Sending request to external API. URL: {}", uriBuilder.toUriString());

        // Выполнение запроса к внешнему API
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"ok".equalsIgnoreCase((String) responseBody.get("status"))) {
            throw new RuntimeException("Failed to purchase phone number: API error");
        }

        if ("long_term".equalsIgnoreCase(rentalType)) {
            Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
            if (message == null) {
                throw new RuntimeException("Failed to purchase phone number: No message in response");
            }
            return processPhoneDetailsForLongTerm(message, user, serviceName, price);
        } else {
            List<Map<String, Object>> messageList = (List<Map<String, Object>>) responseBody.get("message");
            if (messageList.isEmpty()) {
                throw new RuntimeException("Failed to purchase phone number: No numbers available");
            }
            return processPhoneDetails(messageList.get(0), user, serviceName, price);
        }
    }

    private PhoneNumber processPhoneDetailsForLongTerm(Map<String, Object> phoneDetails, User user, String serviceName, Double price) {
        logger.info("Processing long-term phone details: {}", phoneDetails);

        String phoneNumber = (String) phoneDetails.get("mdn");
        String externalId = phoneDetails.getOrDefault("id", "N/A").toString();
        String status = "active";

        updateUserBalance(user, price);

        PhoneNumber phoneNumberEntity = new PhoneNumber();
        phoneNumberEntity.setPhoneNumber(phoneNumber);
        phoneNumberEntity.setExternalId(externalId);
        phoneNumberEntity.setServiceName(serviceName);
        phoneNumberEntity.setStatus(status);
        phoneNumberEntity.setPrice(price);
        phoneNumberEntity.setUser(user);

        if (phoneDetails.containsKey("expires")) {
            String expirationString = (String) phoneDetails.get("expires");
            LocalDateTime expirationDate = LocalDateTime.parse(expirationString,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            phoneNumberEntity.setExpires(expirationDate);
        }

        return phoneNumberRepository.save(phoneNumberEntity);
    }

    private PhoneNumber processPhoneDetails(Map<String, Object> phoneDetails, User user, String serviceName, Double price) {
        logger.info("Processing phone details: {}", phoneDetails);

        String phoneNumber = (String) phoneDetails.get("mdn");
        String externalId = phoneDetails.getOrDefault("id", "N/A").toString();
        String status = "active";

        updateUserBalance(user, price);

        PhoneNumber phoneNumberEntity = new PhoneNumber();
        phoneNumberEntity.setPhoneNumber(phoneNumber);
        phoneNumberEntity.setExternalId(externalId);
        phoneNumberEntity.setServiceName(serviceName);
        phoneNumberEntity.setStatus(status);
        phoneNumberEntity.setPrice(price);
        phoneNumberEntity.setUser(user);

        if (phoneDetails.containsKey("till_expiration")) {
            Integer tillExpiration = (Integer) phoneDetails.get("till_expiration");
            phoneNumberEntity.setExpires(LocalDateTime.now().plusSeconds(tillExpiration));
        }

        return phoneNumberRepository.save(phoneNumberEntity);
    }

    @Transactional
    public void updateUserBalance(User user, Double amount) {
        logger.info("Updating balance for user: {}. Current balance: {}, amount to deduct: {}",
                user.getUsername(), user.getBalance(), amount);

        if (user.getBalance() < amount) {
            logger.error("Insufficient balance. User: {}, current balance: {}, required: {}",
                    user.getUsername(), user.getBalance(), amount);
            throw new RuntimeException("Insufficient balance for user: " + user.getUsername());
        }

        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);

        logger.info("Balance updated successfully. New balance: {}", user.getBalance());
    }

    public List<PhoneNumber> fetchAvailableNumbers(String serviceName) {
        return phoneNumberRepository.findByServiceName(serviceName)
                .stream()
                .filter(phoneNumber -> "AVAILABLE".equalsIgnoreCase(phoneNumber.getStatus()))
                .toList();
    }

    public void transferPhoneNumber(String phoneNumber, String recipientUsername) {
        PhoneNumber phoneNumberEntity = phoneNumberRepository.findByPhoneNumberAndUserUsername(phoneNumber, SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new RuntimeException("Phone number not found or does not belong to the user"));

        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        phoneNumberEntity.setUser(recipient);
        phoneNumberEntity.setStatus("ASSIGNED");
        phoneNumberRepository.save(phoneNumberEntity);
    }

    public List<PhoneNumber> getUserNumbers(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return phoneNumberRepository.findAll().stream()
                .filter(number -> number.getUser() != null && number.getUser().equals(user))
                .collect(Collectors.toList());
    }

    public void assignNumberToUser(Long phoneNumberId, String username) {
        PhoneNumber phoneNumber = phoneNumberRepository.findById(phoneNumberId)
                .orElseThrow(() -> new RuntimeException("Phone number not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        phoneNumber.setUser(user);
        phoneNumber.setStatus("ASSIGNED");
        phoneNumberRepository.save(phoneNumber);
    }

    public void transferNumberByUsername(String donorUsername, String recipientUsername, String phoneNumberValue) {
        User donor = userRepository.findByUsername(donorUsername)
                .orElseThrow(() -> new RuntimeException("Donor not found with username: " + donorUsername));

        // Проверяем, существует ли пользователь-получатель
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient not found with username: " + recipientUsername));

        PhoneNumber phoneNumber = phoneNumberRepository.findByUser(donor).stream()
                .filter(number -> number.getPhoneNumber().equals(phoneNumberValue))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Phone number not found or does not belong to the donor"));

        phoneNumber.setUser(recipient);
        phoneNumberRepository.save(phoneNumber);
    }

    public List<PhoneNumberDTO> getPhoneNumbersByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return phoneNumberRepository.findByUser(user).stream()
                .map(phone -> new PhoneNumberDTO(
                        null,
                        phone.getPhoneNumber(),
                        phone.getServiceName(),
                        phone.getPrice(),
                        phone.getStatus()
                ))
                .collect(Collectors.toList());
    }


    public List<PhoneNumber> getNumbersByOwner(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return phoneNumberRepository.findByUser(owner);
    }

    public void transferNumber(Long numberId, String recipientUsername) {
        PhoneNumber phoneNumber = phoneNumberRepository.findById(numberId)
                .orElseThrow(() -> new RuntimeException("Phone number not found with ID: " + numberId));

        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new RuntimeException("Recipient not found: " + recipientUsername));

        phoneNumber.setUser(recipient);
        phoneNumber.setStatus("ASSIGNED");
        phoneNumberRepository.save(phoneNumber);
    }

    public List<PhoneNumber> getPhoneNumbersByUserId(Long userId) {
        return phoneNumberRepository.findByUserId(userId);
    }


}
