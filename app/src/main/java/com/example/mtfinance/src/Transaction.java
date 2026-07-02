package com.example.mtfinance.src;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction implements Details {
    // instance fields
    private Long id; // placeholder for now
    @NonNull
    private final String name;
    private final String description;
    private final BigDecimal amount;
    @NonNull
    private final LocalDateTime date;

    // constructor
    private Transaction(Builder build) {
        this.id = null;
        this.description = TrackingUtlis.determineDescription(build.description);
        this.date = build.date;
        this.amount = build.amount;
        this.name = build.name;

    }


    public void setId(Long id) {
        this.id = id;
    }

    // getters
    public Long getId() {
        return id;
    }


    public LocalDateTime getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String getDetails() {
        return String.format("Name: %s\n Amount: %s\n desc: %s \n date: %s", name, amount, description, date);
    }




    public static class Builder {

        private String description = TrackingUtlis.EMPTY_DESCRIPTION;
        private final BigDecimal amount; // required
        private final String name; // required
        private LocalDateTime date = LocalDateTime.now();

        public Builder(String name, BigDecimal amount) {
            TrackingUtlis.checkAmount(amount); // cannot be zero or below
            this.amount = amount;
            this.name = name;

            // TODO add functionality that considers transactions like transfers and income (which would be negative).
        }

        public Transaction build() {
            return new Transaction(this);
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