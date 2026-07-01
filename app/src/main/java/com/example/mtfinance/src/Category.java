package com.example.mtfinance.src;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class Category {


    // instance field
    private final String name;
    private final String description;
    private final Set<Transaction> transactions;
    private BigDecimal budget;

    private final Set<Category> children;
    private  Category parent = null;

    // constructor
    public Category(String name, String description, BigDecimal budget) {
        this.name = name;
        this.description = description;
        this.budget = budget;
        this.transactions = new HashSet<>();
        this.children = new HashSet<>();

    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        if (parent != null && !parent.isDescendentOf(this)) {
            this.parent = parent;
        }

    }



    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
        }
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public BigDecimal findTotal(boolean excludeSub) {
        BigDecimal total = new BigDecimal("0");
        Set<Transaction> totalTransactions = new HashSet<>(transactions);
        if (!excludeSub) {
            for (Category child : children) {
                totalTransactions.addAll(child.getTransactions());
            }

        }

        // sum total
        for (Transaction transaction : totalTransactions) {
            total = total.add(transaction.getAmount());
        }
        return total;
    }


    public void setBudget(BigDecimal budget) {
        TrackingUtlis.checkAmount(budget);
        this.budget = budget;
    }

    public boolean isDescendentOf(Category category) {
        if (this.getParent() == null || category.equals(this)) {
            return false;
        }
        else if (category.equals(this.getParent())) {
            return true;
        }


        return this.getParent().isDescendentOf(category);
    }

}