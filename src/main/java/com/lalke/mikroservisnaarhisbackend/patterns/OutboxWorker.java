package com.lalke.mikroservisnaarhisbackend.patterns;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lalke.mikroservisnaarhisbackend.model.OutboxRecord;
import com.lalke.mikroservisnaarhisbackend.repository.OutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxWorker(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void readOutbox() {
        log.debug("OutboxWorker: Checking for pending records...");
        
        List<OutboxRecord> records = outboxRepository.findTop50ByOrderByTimestampAsc();
        
        if (records.isEmpty()) {
            return;
        }

        log.info("OutboxWorker: Found {} records to process", records.size());

        for (OutboxRecord record : records) {
            try {
                // 1. Convert the pre-serialized JSON string from DB into raw bytes
                byte[] body = record.getPayload().getBytes(StandardCharsets.UTF_8);

                // 2. Wrap bytes in a Message object and set the content type header
                // This tells RabbitMQ (and consumers) that this is valid JSON
                Message message = MessageBuilder.withBody(body)
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();

                // 3. Use .send() instead of .convertAndSend()
                // .send() transmits the message body as-is without further serialization
                rabbitTemplate.send(record.getQueue(), message);
                
                log.info("OutboxWorker: Successfully sent record ID: {} to queue: {}", 
                         record.getId(), record.getQueue());
                
                outboxRepository.delete(record);
                
            } catch (Exception e) {
                log.error("OutboxWorker: Failed to process record ID: {}. Error: {}", 
                          record.getId(), e.getMessage());
                // Rethrowing ensures the transaction rolls back for this batch
                throw e; 
            }
        }
    }
}