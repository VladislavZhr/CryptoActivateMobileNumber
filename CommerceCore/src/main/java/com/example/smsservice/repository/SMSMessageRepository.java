package com.example.smsservice.repository;

import com.example.smsservice.model.PhoneNumber;
import com.example.smsservice.model.SMSMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SMSMessageRepository extends JpaRepository<SMSMessage, Long> {
    List<SMSMessage> findByPhoneNumber(PhoneNumber phoneNumber);
    List<SMSMessage> findByPhoneNumber_PhoneNumber(String phoneNumber);
}
