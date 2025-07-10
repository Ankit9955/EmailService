package com.ecommerce.project;

import com.emailservice.model.EmailRequest;
import com.emailservice.model.EmailStatus;
import com.emailservice.provider.EmailProvider;
import com.emailservice.queue.EmailQueue;
import com.emailservice.service.CircuitBreaker;
import com.emailservice.service.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EmailServiceTest {
    @Mock
    private EmailProvider primaryProvider;
    @Mock
    private EmailProvider secondaryProvider;
    @Mock
    private CircuitBreaker circuitBreaker;
    @Mock
    private EmailQueue emailQueue;
    @InjectMocks
    private EmailService emailService;

    private ConcurrentHashMap<String, AtomicInteger> rateLimitMap;
    private final Integer maxRequestsPerMinute = 10;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitMap = new ConcurrentHashMap<>();
        emailService = new EmailService(primaryProvider, secondaryProvider, circuitBreaker, emailQueue, rateLimitMap, maxRequestsPerMinute);
        when(primaryProvider.getProviderName()).thenReturn("MockProviderA");
        when(secondaryProvider.getProviderName()).thenReturn("MockProviderB");
    }

    @Test
    void testSuccessfulEmailSend() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setMessageId("test-123");
        when(circuitBreaker.executeWithCircuitBreaker(primaryProvider, request)).thenReturn(true);

        ResponseEntity<EmailStatus> response = emailService.sendEmail(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailQueue, times(1)).enqueue(request);
    }

    @Test
    void testIdempotency() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setMessageId("test-123");
        when(circuitBreaker.executeWithCircuitBreaker(primaryProvider, request)).thenReturn(true);

        emailService.sendEmail(request);
        ResponseEntity<EmailStatus> response = emailService.sendEmail(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already sent", response.getBody().getErrorMessage());
    }

    @Test
    void testRateLimiting() {
        EmailRequest request = new EmailRequest();
        request.setMessageId("test-123");
        rateLimitMap.put("client-ip", new AtomicInteger(maxRequestsPerMinute));

        ResponseEntity<EmailStatus> response = emailService.sendEmail(request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Rate limit exceeded", response.getBody().getErrorMessage());
    }

    @Test
    void testFallbackProvider() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setMessageId("test-123");
        when(circuitBreaker.executeWithCircuitBreaker(primaryProvider, request)).thenThrow(new RuntimeException("Primary failed"));
        when(circuitBreaker.executeWithCircuitBreaker(secondaryProvider, request)).thenReturn(true);

        emailService.sendEmail(request);

        verify(circuitBreaker, times(1)).executeWithCircuitBreaker(secondaryProvider, request);
    }
}