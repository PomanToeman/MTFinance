package com.example.mtfinance.src.messages;

/**
 * Interface defining message retrieval contracts across all modular message enums.
 */
public interface MessageCli {
    String getMessage(Object... args);
}
