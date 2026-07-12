package com.example.mtfinance.src.trackingengine;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(tableName = "transactions")
public class Transaction implements Details {

    /**
     * Differentiate between expense and income transactions for more accurate tracking.
     */

    // instance fields
    @PrimaryKey(autoGenerate = false)
    private Long transactionId;
    @NonNull
    private final String name;
    private  String description;
    @NonNull
    private final BigDecimal amount;
    @NonNull
    private final LocalDateTime date;
    @NonNull
    private final TrackingType type;

    /**
     * This is for the room database. Use the builder instead when creating new instances.
     *
     */
    public Transaction(@NonNull String name, String description, @NonNull BigDecimal amount, @NonNull LocalDateTime date, @NonNull TrackingType type) {
        TrackingUtlis.checkAmount(amount);
        this.transactionId = TrackingUtlis.getNextTransactionCounterId();
        this.name = name;
        this.description = TrackingUtlis.determineDescription(description);
        this.amount = amount;
        this.date = date;
        this.type = type;
    }

    // constructor
    private Transaction(Builder build) {
        this.transactionId = TrackingUtlis.getNextTransactionCounterId();
        this.description = TrackingUtlis.determineDescription(build.description);
        this.date = build.date;
        this.amount = build.amount;
        this.name = build.name;
        this.type = build.type;

    }

    /**
     * For room database. DO NOT USE.
     *
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    // getters
    public Long getTransactionId() {
        return transactionId;
    }


    @NonNull
    public LocalDateTime getDate() {
        return date;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    @NonNull
    public BigDecimal getAmount() {
        return amount;
    }
    @NonNull
    public TrackingType getType() {
        return type;
    }

    @Override
    public String getDetails() {
        return String.format("Name: %s (%s)\n Amount: %s\n desc: %s \n date: %s", name, type, amount, description, date);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Transaction) {
            Transaction other = (Transaction) o;
            return other.getTransactionId().equals(this.getTransactionId());
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public int hashCode() {
        return transactionId.intValue();
    }

    public static class Builder {

        private String description = TrackingUtlis.EMPTY_DESCRIPTION;
        @NonNull
        private final BigDecimal amount;
        @NonNull
        private final String name;
        private LocalDateTime date = LocalDateTime.now();
        private TrackingType type = TrackingType.EXPENSE;

        /**
         * Constructor of the builder. Must have the required fields.
         * @param name - The name of the transaction.
         * @param amount - The dollar amount of the transaction.
         */
        public Builder(@NonNull String name, @NonNull BigDecimal amount) {
            TrackingUtlis.checkAmount(amount);
            this.amount = amount;
            this.name = name;


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

        public Builder date(LocalDate date) {
            this.date = date.atStartOfDay();
            return this;
        }

        public Builder type(TrackingType type) {
            this.type = type;
            return this;
        }




    }
}