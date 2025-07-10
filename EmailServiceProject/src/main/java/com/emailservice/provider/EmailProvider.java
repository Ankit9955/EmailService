package com.emailservice.provider;

import com.emailservice.model.EmailRequest;

public interface EmailProvider {
    boolean sendEmail(EmailRequest request) throws Exception;
    String getProviderName();
}