package com.emailservice.model;

import lombok.Data;

@Data
public class EmailRequest {
    private String messageId;
    private String to;
    private String subject;
    private String body;
}