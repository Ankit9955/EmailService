package com.emailservice.provider;

import com.emailservice.model.EmailRequest;
import org.springframework.stereotype.Component;

@Component("mockProviderA")
public class MockProviderA implements EmailProvider {
    @Override
    public boolean sendEmail(EmailRequest request) throws Exception {
        if (Math.random() < 0.3) {
            throw new Exception("MockProviderA failed to send email");
        }
        return true;
    }

    @Override
    public String getProviderName() {
        return "MockProviderA";
    }
}