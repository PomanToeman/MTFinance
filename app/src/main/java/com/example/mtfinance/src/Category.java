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

    public BigDecimal getBudget() {
        return budget;
    }

    public void setParent(Category parent) {
        if (this.equals(parent)) {
            return;
        }

        // parent can not already be a descendant nor an ancestor.
        if (parent != null && !parent.isDescendantOf(this) && !this.isDescendantOf(parent)) {
            parent.addChild(this);
            this.parent = parent;

        }

    }


    private void addChild(Category child) {
        // private to disallow infinite loop.

        // Child must not already be a descendant nor ancestor.
        if (!child.isDescendantOf(this) && !this.isDescendantOf(child)) {
            children.add(child);
        }

        // checks if budget is above
        determineMinimumBudget();

    }

    public Set<Category> getChildren(boolean includeGrand) {
        Set<Category> copy = new HashSet<>(children); // fresh copy

        if (includeGrand) {
            for (Category child : children) {
                copy.addAll(child.getChildren(true));
            }
        }
        return copy;
    }

    public BigDecimal determineMinimumBudget() {
        // determine the absolute minimum budget considering sub categories' budgets, will set to that budget if current is below.
        BigDecimal total = BigDecimal.valueOf(0);
        for (Category child : getChildren(false)) { // this assumes children budgets are already at minimum budget
            total = total.add(child.getBudget());
        }

        // will automatically set budget if current budget is below minimum
        if (budget.compareTo(total) < 0) {
            this.budget = total;
            // since this budget has changed
            if (parent != null) {
                parent.determineMinimumBudget();
            }
        }


        return total;
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
        BigDecimal minimum = determineMinimumBudget();

        if (budget.compareTo(minimum) > 0) {
            this.budget = budget;

            // recheck if parent budget is now below minimum.
            if (parent != null) {
                parent.determineMinimumBudget();
            }
        }

    }

    public boolean isDescendantOf(Category category) {
        if (this.getParent() == null || category.equals(this)) {
            return false;
        }
        else if (category.equals(this.getParent())) {
            return true;
        }


        return this.getParent().isDescendantOf(category); // recursive.
    }

}