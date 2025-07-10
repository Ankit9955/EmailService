package com.emailservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import com.emailservice.model.EmailRequest;
import com.emailservice.provider.EmailProvider;

@Component
public class CircuitBreaker {
    @CircuitBreaker(name = "emailService", fallbackMethod = "fallback")
    public boolean executeWithCircuitBreaker(EmailProvider provider, EmailRequest request) throws Exception {
        return provider.sendEmail(request);
    }

    public boolean fallback(EmailProvider provider, EmailRequest request, Throwable t) {
        return false;
    }
}