package com.example.mtfinance.src;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
@Entity(
        tableName = "categories",
        indices = {@Index(value = {"name"}, unique = true)} )
public class Category implements Details {


    // instance fields
    @PrimaryKey (autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private final String name;
    private final String description;
    @Ignore
    private final Set<Transaction> transactions;
    private BigDecimal budget; // assume monthly budget

    @Ignore
    private final Set<Category> children;
    @Ignore
    private  Category parent = null; // default

    // constructor
    public Category(@NonNull String name,  String description, @NonNull BigDecimal budget) {
        TrackingUtlis.checkAmount(budget);

        this.name = name;
        this.description = TrackingUtlis.determineDescription(description);
        this.budget = budget;
        this.transactions = new HashSet<>();
        this.children = new HashSet<>();


    }

    // setters/adders


    public void setId(long id) {
        this.id = id;
    }

    public void setParent(@NonNull Category parent) {
        if (this.equals(parent)) {
            return;
        }

        // parent can not already be a descendant nor an ancestor.
        if (!parent.isDescendantOf(this) && !this.isDescendantOf(parent)) {
            parent.addChild(this);
            this.parent = parent;

        }

    }

    public void removeTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.remove(transaction);
        }
    }

    public void makeChildrenCongruent() {
        // this makes children at the same level as the parent.
        if (parent != null) {
            for (Category child : children) {
                children.remove(child);
                child.setParentInternal(parent);
            }

        }
    }

    private void setParentInternal(Category parent) {
        // this for makeChildrenCongruent (bypasses checks)
        if (this.equals(parent)) {
            return;
        }
        this.parent = parent;
        parent.addChild(this);

    }



    private void addChild(@NonNull Category child) {
        // private as your suppose to set parent first.
        children.add(child);
        // checks if budget is above
        determineMinimumBudget();

    }
    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
        }
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


    // getters


    public String getName() {
        return name;
    }

    public Category getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public long getId() {
        return id;
    }

    public Set<Category> getChildren(boolean includeGrand) {
        Set<Category> copy = new HashSet<>(children); // fresh copy

        // adds ALL grandchildren and further.
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


        return total; // can be used for comparison
    }


    public Set<Transaction> getTransactions(boolean includeSub) {
        Set<Transaction> totalTransactions = new HashSet<>(transactions);

        // include all children and further transactions (should ignore duplicates)
        if (includeSub) {
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




    public boolean isDescendantOf(Category category) {
        if (this.getParent() == null || category.equals(this)) {
            return false;
        }
        else if (category.equals(this.getParent())) {
            return true;
        }


        return this.getParent().isDescendantOf(category); // recursive.
    }

    @Override
    public String getDetails() {
        return String.format("Name: %s\nDescription: %s\nSub Categories: %s", name, description, children);
    }


    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

}