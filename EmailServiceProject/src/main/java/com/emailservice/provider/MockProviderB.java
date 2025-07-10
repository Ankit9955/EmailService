package com.emailservice.provider;

import com.emailservice.model.EmailRequest;
import org.springframework.stereotype.Component;

@Component("mockProviderB")
public class MockProviderB implements EmailProvider {
    @Override
    public boolean sendEmail(EmailRequest request) throws Exception {
        if (Math.random() < 0.2) {
            throw new Exception("MockProviderB failed to send email");
        }
        return true;
    }

    @Override
    public String getProviderName() {
        return "MockProviderB";
    }
}