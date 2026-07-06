package com.example.mtfinance.src;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "categoryTransactionCrossRef",
        primaryKeys = {"categoryId", "transactionId"},
        indices = {@Index(value = {"categoryId"}), @Index(value = {"transactionId"})})
public class CategoryTransactionCrossRef {

    @NonNull
    private Long categoryId;
    @NonNull
    private Long transactionId;

    // constructor
    public CategoryTransactionCrossRef(@NonNull Long categoryId,@NonNull Long transactionId) {
        this.categoryId = categoryId;
        this.transactionId = transactionId;
    }

    // getters
    @NonNull
    public Long getCategoryId() {
        return categoryId;
    }
    @NonNull
    public Long getTransactionId() {
        return transactionId;
    }

    // setters
    public void setCategoryId(@NonNull Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setTransactionId(@NonNull Long transactionId) {
        this.transactionId = transactionId;
    }
}
