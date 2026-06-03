package com.lalke.mikroservisnaarhisbackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    private String recipient;
    private String text;

    //{"recipient": "test@test.com", "text": "aaaaaaa"}
}
