package com.example.mtfinance.src;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class Category {
    public static final Category ROOT_CATEGORY = new Category("General category", "The root of all categories, meant for general tracking", BigDecimal.ONE);

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

        if (!this.equals(ROOT_CATEGORY)) {
            this.parent = ROOT_CATEGORY;
        }
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        if (this.equals(ROOT_CATEGORY)) {
            return;
        }
        if (parent != null && !parent.isDescendentOf(this)) {
            parent.addChild(this);
            this.parent = parent;

        }

    }


    private void addChild(Category child) {
        // private to disallow infinite loop.

        // Child must not already be a descendant or ancestor.
        if (!child.isDescendentOf(this) && !this.isDescendentOf(child)) {
            children.add(child);
        }

    }



    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
        }
    }

    public Set<Transaction> getTransactions(boolean includesub) {
        Set<Transaction> totalTransactions = new HashSet<>(transactions);
        if (includesub) {
            for (Category child : children) {
                totalTransactions.addAll(child.getTransactions(true));
            }

        }

        return totalTransactions;
    }

    public BigDecimal findTotal(boolean includeSub) {
        // find total of the transactions within a category.
        BigDecimal total = new BigDecimal("0");
        Set<Transaction> totalTransactions = getTransactions(includeSub);

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


        return this.getParent().isDescendentOf(category); // recursive.
    }

}