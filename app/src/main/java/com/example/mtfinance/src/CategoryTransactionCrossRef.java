package com.example.mtfinance.src;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "category_transaction_cross_ref",
        primaryKeys = {"category_id", "transaction_id"},
        indices = {@Index(value = {"category_id"}), @Index(value = {"transaction_id"})})
public class CategoryTransactionCrossRef {

    @NonNull
    private Long category_id;
    @NonNull
    private Long transaction_id;

    // constructor
    public CategoryTransactionCrossRef(@NonNull Long category_id,@NonNull Long transaction_id) {
        this.category_id = category_id;
        this.transaction_id = transaction_id;
    }

    // getters
    @NonNull
    public Long getCategory_id() {
        return category_id;
    }
    @NonNull
    public Long getTransaction_id() {
        return transaction_id;
    }

    // setters
    public void setCategory_id(@NonNull Long category_id) {
        this.category_id = category_id;
    }

    public void setTransaction_id(@NonNull Long transaction_id) {
        this.transaction_id = transaction_id;
    }
}
