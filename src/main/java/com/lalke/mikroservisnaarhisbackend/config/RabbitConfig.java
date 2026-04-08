package com.lalke.mikroservisnaarhisbackend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean public Queue locationGetAllQueue() { return new Queue("location.get.all", true); }
    @Bean public Queue locationGetAllResponseQueue() { return new Queue("location.get.all.res", true); }
    @Bean public Queue locationGetByIdQueue() { return new Queue("location.get.id", true); }
    @Bean public Queue locationGetByIdResponseQueue() { return new Queue("location.get.id.res", true); }
    @Bean public Queue locationSaveQueue() { return new Queue("location.save", true); }
    @Bean public Queue locationDeleteQueue() { return new Queue("location.delete", true); }

    @Bean public Queue eventGetAllQueue() { return new Queue("event.get.all", true); }
    @Bean public Queue eventGetAllResponseQueue() { return new Queue("event.get.all.res", true); }
    @Bean public Queue eventGetByIdQueue() { return new Queue("event.get.id", true); }
    @Bean public Queue eventGetByIdResponseQueue() { return new Queue("event.get.id.res", true); }
    @Bean public Queue eventSaveQueue() { return new Queue("event.save", true); }
    @Bean public Queue eventDeleteQueue() { return new Queue("event.delete", true); }

    @Bean public Queue speakerGetAllQueue() { return new Queue("speaker.get.all", true); }
    @Bean public Queue speakerGetAllResponseQueue() { return new Queue("speaker.get.all.res", true); }
    @Bean public Queue speakerGetByIdQueue() { return new Queue("speaker.get.id", true); }
    @Bean public Queue speakerGetByIdResponseQueue() { return new Queue("speaker.get.id.res", true); }
    @Bean public Queue speakerSaveQueue() { return new Queue("speaker.save", true); }
    @Bean public Queue speakerDeleteQueue() { return new Queue("speaker.delete", true); }

    @Bean
    public JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}