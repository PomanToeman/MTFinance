package com.example.mtfinance.src;

public class Transaction {
    // instance fields
    private final Integer Id;
    private String description;
    private final long date;

    // constructor
    public Transaction(Integer Id, String description, long date) {
        this.Id = Id;
        this.description = description;
        this.date = date;
    }

    public Integer getId() {
        return Id;
    }




}