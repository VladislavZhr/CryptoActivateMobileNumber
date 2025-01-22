package com.example.smsservice.service;

import com.example.smsservice.model.Markup;
import com.example.smsservice.repository.MarkupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarkupService {

    private final MarkupRepository markupRepository;

    public Markup getCurrentMarkup() {
        return markupRepository.findAll().stream().findFirst().orElseGet(() -> {
            Markup defaultMarkup = new Markup();
            defaultMarkup.setValue(0.0);
            return markupRepository.save(defaultMarkup);
        });
    }

    public Markup updateMarkup(double value) {
        Markup markup = getCurrentMarkup();
        markup.setValue(value);
        return markupRepository.save(markup);
    }
}
