package com.example.mtfinance.src;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(tableName = "transactions")
public class Transaction implements Details {
    // instance fields
    @PrimaryKey(autoGenerate = false)
    private Long id;
    @NonNull
    private final String name;
    private final String description;
    @NonNull
    private final BigDecimal amount;
    @NonNull
    private final LocalDateTime date;

    // for room database
    public Transaction(@NonNull String name, String description, @NonNull BigDecimal amount, @NonNull LocalDateTime date) {
        TrackingUtlis.checkAmount(amount);
        this.id = TrackingUtlis.getNextTransactionCounterId();
        this.name = name;
        this.description = TrackingUtlis.determineDescription(description);
        this.amount = amount;
        this.date = date;
    }

    // constructor
    private Transaction(Builder build) {
        this.id = TrackingUtlis.getNextTransactionCounterId();
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
        @NonNull
        private final BigDecimal amount; // required
        @NonNull
        private final String name; // required
        private LocalDateTime date = LocalDateTime.now();

        public Builder(@NonNull String name, @NonNull BigDecimal amount) {
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