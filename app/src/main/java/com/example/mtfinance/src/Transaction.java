package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

public class Transaction {
    // instance fields
    private final Integer Id;
    private final String description;
    private final float amount;
    private final LocalDateTime date;

    // constructor
    public Transaction(Integer Id, String description, LocalDateTime date, float amount) {
        this.Id = Id;
        this.description = description;
        this.date = date;
        this.amount = amount;
    }

    public Integer getId() {
        return Id;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Id: %s\n desc: %s \n date: %s", Id, description, date);
    }




}