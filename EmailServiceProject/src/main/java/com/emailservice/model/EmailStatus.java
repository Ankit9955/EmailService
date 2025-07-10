package com.emailservice.model;

import lombok.Data;

@Data
public class EmailStatus {
    private String messageId;
    private String status;
    private String provider;
    private int attemptCount;
    private String errorMessage;
}