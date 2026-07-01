package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction implements details {
    // instance fields
    private final Integer Id;
    private final String name;
    private final String description;
    private final BigDecimal amount;
    private final LocalDateTime date;

    // constructor
    private Transaction(Builder build) {
        this.Id = build.Id;
        this.description = build.description;
        this.date = build.date;
        this.amount = build.amount;
        this.name = build.name;

    }

    public Integer getId() {
        return Id;
    }



    @Override
    public String getDetails() {
        return String.format("Name: %s\n Amount: %s\n desc: %s \n date: %s", name, amount, description, date);
    }

    public BigDecimal getAmount() {
        return amount;
    }


    public static class Builder {
        private  Integer Id = 1;
        private String description = "No description";
        private BigDecimal amount; // required
        private String name; // required
        private LocalDateTime date = LocalDateTime.now();

        public Builder(String name, BigDecimal amount) {
            this.amount = amount;
            this.name = name;
        }

        public Transaction build() {
            return new Transaction(this);
        }

        public Builder Id(Integer Id) {
            this.Id = Id;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder date(LocalDateTime date) {
            this.date = date;
            return this;
        }
    }
}