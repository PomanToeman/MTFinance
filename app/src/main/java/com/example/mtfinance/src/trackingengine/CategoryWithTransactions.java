package com.example.mtfinance.src.trackingengine;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.example.mtfinance.src.repositories.roomdatabase.CategoryTransactionCrossRef;


import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * POJO. Defines the relation between Categories and Transactions (many-to-many).
 */
public class CategoryWithTransactions implements Details {
    @Embedded
    public Category category;

    @Relation(
            parentColumn = "categoryId",
            entityColumn = "transactionId",
            associateBy = @Junction(CategoryTransactionCrossRef.class)
    )
    public Set<Transaction> transactions;

   public CategoryWithTransactions() {
       transactions = new HashSet<>();
   }

    /**
     * This only does not account for sub-categories
     * @return
     */
   public BigDecimal findTotal() {
       BigDecimal total = new BigDecimal("0");
       for (Transaction transaction : transactions) {
           total = total.add(transaction.getAmount());
       }
       return total;
   }

   @Override
    public String getDetails() {
       return category.getDetails() + "\n" + transactions + "\nTotal: " + findTotal();
   }







}
