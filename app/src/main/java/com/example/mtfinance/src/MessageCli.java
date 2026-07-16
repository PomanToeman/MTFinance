package com.example.mtfinance.src;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum MessageCli {
    TRANSACTION_READ_ERROR("");



    private final String message;

    private static final Map<String, Set<String>> phaseMap = new HashMap<>();

    private MessageCli(String message) {
        this.message = message;
    }


    public String getMessage(String... args) {

        return message;
    }


}
