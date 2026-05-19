package com.monow.domain.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "MONOW";
    private static final int ACCOUNT_NUMBER_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");

    public String generate() {
        String datePart = LocalDateTime.now().format(formatter);

        StringBuilder randomNumber = new StringBuilder();
        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            randomNumber.append(random.nextInt(10));

        }

        return PREFIX + datePart + randomNumber.toString();
    }
}
