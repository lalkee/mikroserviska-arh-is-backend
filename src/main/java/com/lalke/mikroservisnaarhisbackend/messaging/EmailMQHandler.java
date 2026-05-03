package com.lalke.mikroservisnaarhisbackend.messaging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Email;

import lombok.RequiredArgsConstructor;

@Component
public class EmailMQHandler {
        
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();    
    int count = 0;
    int threshold = 3;

    public EmailMQHandler() {
        scheduler.scheduleAtFixedRate(() -> {
        synchronized (this) {
            count = 0;
            this.notifyAll();
        }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @RabbitListener(queues = "email.send")
    public void send(Email email) throws InterruptedException                  
    {
        synchronized (this){
            while(count >= threshold) {
                this.wait();
            }
            count++;
        }

        System.out.println("FOR: " + email.getRecipient() + " TEXT: " + email.getText());
    }

}
