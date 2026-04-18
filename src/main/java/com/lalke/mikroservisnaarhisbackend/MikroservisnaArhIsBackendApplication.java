package com.lalke.mikroservisnaarhisbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MikroservisnaArhIsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MikroservisnaArhIsBackendApplication.class, args);
    }

}
