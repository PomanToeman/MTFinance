package com.example.mtfinance.src.trackingengine;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.example.mtfinance.src.roomdatabase.CategoryTransactionCrossRef;


import java.util.Set;

/**
 * POJO. Defines the relation between Categories and Transactions (many-to-many).
 */
public class CategoryWithTransactions {
    @Embedded
    public Category category;

    @Relation(
            parentColumn = "id",
            entityColumn = "transactionId",
            associateBy = @Junction(CategoryTransactionCrossRef.class)
    )
    public Set<Transaction> transactions;
}
