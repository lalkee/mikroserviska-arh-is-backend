package com.lalke.mikroservisnaarhisbackend.exceptions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/toggle")
public class ErrorSimulator {

    private final AtomicBoolean errorActive = new AtomicBoolean(false);

    @GetMapping
    public String toggle() {
        boolean newState = !errorActive.get();
        errorActive.set(newState);
        
        return newState ? "service innactive" : "service active";
    }

    public void throwTriggeredException() {
        if (errorActive.get()) {
            throw new RuntimeException("Simulated System Failure");
        }
    }

    public void longOperation() throws InterruptedException {
        if (errorActive.get()) {
            Thread.sleep(10000);
        }
    }
}
