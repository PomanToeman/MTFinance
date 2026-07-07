package com.example.mtfinance.src.trackingengine;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
@Entity(
        tableName = "categories",
        indices = {@Index(value = {"name"}, unique = true)} )
public class Category implements Details {


    // instance fields
    @PrimaryKey (autoGenerate = false)
    private Long categoryId;

    @NonNull
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    private  String name;
    private  String description;

    private BigDecimal monthlyBudget;
    @ColumnInfo(name = "parent_id")
    private Long parentId = null; // foreign key

    // cache fields (need to be restored/updated once extracted from room)
    @Ignore
    private final Set<Category> children;
    @Ignore
    private  Category parent = null;


    // constructor
    public Category(@NonNull String name,  String description, @NonNull BigDecimal monthlyBudget) {
        TrackingUtlis.checkAmount(monthlyBudget);

        this.categoryId = TrackingUtlis.getNextCategoryCounterId();
        this.name = name;
        this.description = TrackingUtlis.determineDescription(description);
        this.monthlyBudget = monthlyBudget;

        this.children = new HashSet<>();


    }

    // setters/adders


    public void setCategoryId(@NonNull Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setDescription(@NonNull String description) {
        this.description = TrackingUtlis.determineDescription(description);
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }



    public void setParentId(@NonNull Long parentId) {
        this.parentId = parentId;
    }

    /**
     * To set this category as the sub category (child) of a given category (parent) to allow dependence and more accurate tracking.
     * This is through a tree structure (one parent to many children).
     * The parent cannot be already be descendant of this category as would break the structure.
     * if given category is a grandparent or above, this category will move up until it is a child to preserve the structure.
     * @param parent - the parent to be set for this category (if possible)
     */
    public void setParent(@NonNull Category parent) {
        if (this.equals(parent)) {
            return;
        }

        if (!parent.isDescendantOf(this) && !this.isDescendantOf(parent)) {
            parent.addChild(this);
            this.parent = parent;
            this.parentId = parent.getCategoryId();

            return;

        }

        if (this.isDescendantOf(parent)) {
            this.getParent().removeChild(this);
            this.setParentInternal(parent);
        }

    }

    /**
     * Helper method.
     * @param child
     */
    private void removeChild(@NonNull Category child) {
        children.remove(child);
    }


    /**
     * Meant to make Children (sub-categories) the same level of the parent in the tree structure, so the parent can be safely modified/deleted without
     * affecting their previous children.
     * Warning: This should be only be done mainly for deletion. Cannot be undone (without advance moves).
     *
     */
    public void makeChildrenCongruent() {

        if (parent != null) {
            for (Category child : children) {
                children.remove(child);
                child.setParentInternal(parent);
            }

        }
    }

    /**
     * This helper method is to set parent while bypassing checks.
     * @param parent
     */
    private void setParentInternal(Category parent) {

        if (this.equals(parent)) {
            return;
        }
        this.parent = parent;
        this.parentId = parent.getCategoryId();
        parent.addChild(this);

    }


    /**
     * Helper method. Automatically checks this category's budget as the minimum budget changed.
     * @param child - the child Category to be added.
     */
    private void addChild(@NonNull Category child) {
        children.add(child);
        determineMinimumBudget();

    }


    /**
     * Warning: The budget should be at least above the minimum budget, otherwise it will be ignored.
     * @param monthlyBudget - the given budget to be set (if above minimum).
     * @throws IllegalArgumentException if the given budget is invalid amount.
     */

    public void setMonthlyBudget(BigDecimal monthlyBudget) throws IllegalArgumentException {
        TrackingUtlis.checkAmount(monthlyBudget);
        BigDecimal minimum = determineMinimumBudget();

        if (monthlyBudget.compareTo(minimum) > 0) {
            this.monthlyBudget = monthlyBudget;

            // recheck if parent budget is now below minimum.
            if (parent != null) {
                parent.determineMinimumBudget();
            }
        }

    }


    // getters

    public Long getParentId() {
        return parentId;
    }


    public String getName() {
        return name;
    }

    /**
    * Warning: Ensure the parent is cached before calling this method.
     */
    public Category getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public Long getCategoryId() {
        return categoryId;
    }




    /**
     * Produces a defensive copy of children to ensure immutability.
     * Warning: You must cache the children first before calling this method if extracted from room.
     *
     * @param includeGrand - include the grand children and below if true. (ensure cached is up to date)
     * @return returns the children of the category as a set.
     */
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

    /**
     * determine the absolute minimum budget considering the sum of the sub categories' budgets.
     * It only considers the immediate children's budgets (as we assume they are already at minimum).
     * It will also automatically update the parent's budget if below minimum.
     *
     * Warning: Ensure the children are cached before calling this method.
     *
     * @return the absolute minimum budget. Can be used for comparison
     */
    public BigDecimal determineMinimumBudget() {

        BigDecimal total = BigDecimal.valueOf(0);
        for (Category child : getChildren(false)) {
            total = total.add(child.getMonthlyBudget());
        }


        if (monthlyBudget.compareTo(total) < 0) {
            this.monthlyBudget = total;

            if (parent != null) {
                parent.determineMinimumBudget();
            }
        }


        return total;
    }




    /**
     * This is to check whether this category is a descendant of a given category via recursion.
     *
     * Warning: if extracted from room, ensure intial parents are cached/restored before calling this method.
     *
     *
     * @param category - the given category to check against this category (aka potential ancestor).
     * @return returns true if the category is a descendant of this category. false otherwise
     */
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

    @Override
    public int hashCode() {
        return categoryId.intValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Category) {
            Category other = (Category) o;
            return other.getCategoryId().equals(this.getCategoryId());
        }
        return false;
    }


}