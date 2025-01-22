package com.example.smsservice.util;

import com.example.smsservice.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean result = path.equals("/test-email/send") || path.equals("/favicon.ico") ||
                path.equals("/api/auth/login") || path.equals("/api/auth/register") ||
                path.equals("/api/auth/forgot-password") ||
                path.equals("/api/payments/callback") || path.equals("/api/proxy/services") || path.equals("/api/auth/password-update");
        System.out.println("Path: " + path + ", shouldNotFilter: " + result);
        return result;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        System.out.println("Processing request: " + path);

        System.out.println("Request Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println(headerName + ": " + request.getHeader(headerName));
        }

        if (shouldNotFilter(request)) {
            System.out.println("Skipping filter for path: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);
            System.out.println("Extracted token: " + token);
            if (token != null) {
                authenticateToken(token, request);
                System.out.println("Token authenticated for path: " + path);
            } else {
                throw new SecurityException("Token is missing");
            }
        } catch (SecurityException e) {
            System.err.println("Security exception: " + e.getMessage());
            handleError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            handleError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            System.out.println("Authorization header found.");
            return authorizationHeader.substring(7);
        }
        System.out.println("No Authorization header found.");
        return null;
    }

    private void authenticateToken(String token, HttpServletRequest request) {
        System.out.println("Authenticating token: " + token);
        String username = jwtUtil.extractUsername(token);
        System.out.println("Extracted username: " + username);

        if (username == null) {
            throw new SecurityException("Token does not contain username");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        System.out.println("Loaded UserDetails: " + userDetails);

        if (jwtUtil.validateToken(token, userDetails.getUsername())) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            System.out.println("User authenticated: " + username);
        } else {
            throw new SecurityException("Invalid token for user: " + username);
        }
    }

    private void handleError(HttpServletResponse response, int status, String message) throws IOException {
        System.out.println("Handling error. Status: " + status + ", Message: " + message);
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
