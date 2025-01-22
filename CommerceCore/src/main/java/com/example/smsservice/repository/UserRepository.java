package com.example.smsservice.repository;

import com.example.smsservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    default void updateBalance(User user, double newBalance) {
        user.setBalance(newBalance);
        save(user);
    }

}
