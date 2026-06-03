package com.lalke.mikroservisnaarhisbackend.patterns;

import java.util.concurrent.Callable;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RetryDLQ {
    private static final String DEFAULT_DLQ = "general.dlq";
    private final Retry retry;
    private final RabbitTemplate rabbitTemplate;

    public RetryDLQ(Retry retry, RabbitTemplate rabbitTemplate) {
        this.retry = retry;
        this.rabbitTemplate = rabbitTemplate;
    }

    public <T, R> R executeWithDlq(T payload, String originalRoutingKey, Callable<R> action) throws Exception {
        return executeWithDlq(payload, originalRoutingKey, DEFAULT_DLQ, action);
    }

    public <T, R> R executeWithDlq(T payload, String originalRoutingKey, String dlqRoutingKey, Callable<R> action) throws Exception {
        try {
            return retry.execute(action);
        } catch (Exception e) {
            sendToDeadLetterQueue(payload, originalRoutingKey, dlqRoutingKey, e);
            throw e;
        }
    }

    private <T> void sendToDeadLetterQueue(T payload, String originalRoutingKey, String routingKey, Exception cause) {
        rabbitTemplate.convertAndSend(routingKey, payload, message -> {
            message.getMessageProperties().getHeaders().put("x-exception-message", cause.getMessage());
            message.getMessageProperties().getHeaders().put("x-original-routing-key", originalRoutingKey);
            return message;
        });
    }
}