/*package com.example.smsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class HttpsRedirectConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .requiresChannel(channel -> channel.anyRequest().requiresSecure()) // Redirect HTTP to HTTPS
                .csrf(csrf -> csrf.disable()); // Optionally disable CSRF for simplicity in APIs (not recommended for production)

        return http.build();
    }
}
*/