package com.example.smsservice.repository;

import com.example.smsservice.model.PhoneNumber;
import com.example.smsservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {

    List<PhoneNumber> findByServiceName(String serviceName);
    Optional<PhoneNumber> findByPhoneNumberAndUserUsername(String phoneNumber, String username);
    List<PhoneNumber> findByUser(User owner);
    List<PhoneNumber> findByUserId(Long userId);
    Optional<PhoneNumber> findByPhoneNumber(String phoneNumber);
}
