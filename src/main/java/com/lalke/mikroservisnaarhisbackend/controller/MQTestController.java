package com.lalke.mikroservisnaarhisbackend.controller;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

@RestController
public class MQTestController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Define the queue if it doesn't exist
    @Bean
    public Queue queueToReact() {
        return new Queue("spring_to_react", true);
    }

    @Bean
    public Queue queueFromReact() {
        return new Queue("react_to_spring", true);
    }

    // Endpoint to send message via CURL: curl -X POST http://localhost:8080/send?msg=Hello
    @PostMapping("/send")
    public String sendMessage(@RequestParam String msg) {
        rabbitTemplate.convertAndSend("spring_to_react", msg);
        return "Message sent to RabbitMQ: " + msg;
    }

    // Listener that prints received messages to the console
    @RabbitListener(queues = "react_to_spring")
    public void receiveMessage(String message) {
        System.out.println(" [x] Received in Spring: " + message);
    }
}