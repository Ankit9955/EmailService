package com.emailservice.service;

import com.emailservice.model.EmailRequest;
import com.emailservice.model.EmailStatus;
import com.emailservice.provider.EmailProvider;
import com.emailservice.queue.EmailQueue;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RestController
@RequestMapping("/api/email")
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final EmailProvider primaryProvider;
    private final EmailProvider secondaryProvider;
    private final CircuitBreaker circuitBreaker;
    private final EmailQueue emailQueue;
    private final ConcurrentHashMap<String, AtomicInteger> rateLimitMap;
    private final Integer maxRequestsPerMinute;
    private final Map<String, EmailStatus> statusTracker = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sentEmails = new ConcurrentHashMap<>();

    @Autowired
    public EmailService(
            @Qualifier("mockProviderA") EmailProvider primaryProvider,
            @Qualifier("mockProviderB") EmailProvider secondaryProvider,
            CircuitBreaker circuitBreaker,
            EmailQueue emailQueue,
            ConcurrentHashMap<String, AtomicInteger> rateLimitMap,
            Integer maxRequestsPerMinute) {
        this.primaryProvider = primaryProvider;
        this.secondaryProvider = secondaryProvider;
        this.circuitBreaker = circuitBreaker;
        this.emailQueue = emailQueue;
        this.rateLimitMap = rateLimitMap;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @PostConstruct
    public void init() {
        logger.info("EmailService initialized");
    }

    @PostMapping("/send")
    public ResponseEntity<EmailStatus> sendEmail(@RequestBody EmailRequest request) {
        if (sentEmails.containsKey(request.getMessageId())) {
            EmailStatus status = statusTracker.get(request.getMessageId());
            status.setErrorMessage("Email already sent");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(status);
        }

        String clientIp = "client-ip";
        AtomicInteger requestCount = rateLimitMap.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        if (requestCount.incrementAndGet() > maxRequestsPerMinute) {
            EmailStatus status = new EmailStatus();
            status.setMessageId(request.getMessageId());
            status.setStatus("FAILED");
            status.setErrorMessage("Rate limit exceeded");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(status);
        }

        emailQueue.enqueue(request);
        processQueueAsync();
        EmailStatus status = statusTracker.getOrDefault(request.getMessageId(), new EmailStatus());
        return ResponseEntity.ok(status);
    }

    @Async
    public void processQueueAsync() {
        try {
            EmailRequest request = emailQueue.dequeue();
            EmailStatus status = new EmailStatus();
            status.setMessageId(request.getMessageId());
            status.setAttemptCount(0);
            statusTracker.put(request.getMessageId(), status);
            sendEmailWithRetry(request, status);
        } catch (InterruptedException e) {
            logger.error("Queue processing interrupted", e);
        }
    }

    @Retry(name = "emailService", fallbackMethod = "fallbackSend")
    private void sendEmailWithRetry(EmailRequest request, EmailStatus status) {
        status.setAttemptCount(status.getAttemptCount() + 1);
        try {
            boolean result = circuitBreaker.executeWithCircuitBreaker(primaryProvider, request);
            if (result) {
                status.setStatus("SUCCESS");
                status.setProvider(primaryProvider.getProviderName());
                sentEmails.put(request.getMessageId(), true);
                logger.info("Email {} sent successfully via {}", request.getMessageId(), primaryProvider.getProviderName());
            } else {
                status.setErrorMessage("Failed to send email via primary provider");
                throw new RuntimeException("Failed to send email via primary provider");
            }
        } catch (Exception e) {
            status.setErrorMessage(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void fallbackSend(EmailRequest request, EmailStatus status, Throwable t) {
        status.setAttemptCount(status.getAttemptCount() + 1);
        try {
            boolean result = circuitBreaker.executeWithCircuitBreaker(secondaryProvider, request);
            if (result) {
                status.setStatus("SUCCESS");
                status.setProvider(secondaryProvider.getProviderName());
                sentEmails.put(request.getMessageId(), true);
                logger.info("Email {} sent successfully via {}", request.getMessageId(), secondaryProvider.getProviderName());
            } else {
                status.setStatus("FAILED");
                status.setErrorMessage("Both providers failed");
                logger.error("Email {} failed to send", request.getMessageId());
            }
        } catch (Exception e) {
            status.setStatus("FAILED");
            status.setErrorMessage(e.getMessage());
            logger.error("Email {} failed to send via secondary provider", request.getMessageId());
        }
    }
}