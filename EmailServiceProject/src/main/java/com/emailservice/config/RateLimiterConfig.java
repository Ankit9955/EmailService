package com.emailservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimiterConfig {
    private final ConcurrentHashMap<String, AtomicInteger> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    @Bean
    public ConcurrentHashMap<String, AtomicInteger> rateLimitMap() {
        return rateLimitMap;
    }

    @Bean
    public Integer maxRequestsPerMinute() {
        return MAX_REQUESTS_PER_MINUTE;
    }
}