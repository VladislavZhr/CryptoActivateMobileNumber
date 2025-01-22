package com.example.smsservice.repository;

import com.example.smsservice.model.Markup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkupRepository extends JpaRepository<Markup, Long> {
}
