package com.emailservice.queue;

import com.emailservice.model.EmailRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class EmailQueue {
    private final BlockingQueue<EmailRequest> queue = new LinkedBlockingQueue<>();

    public void enqueue(EmailRequest request) {
        queue.offer(request);
    }

    public EmailRequest dequeue() throws InterruptedException {
        return queue.take();
    }
}