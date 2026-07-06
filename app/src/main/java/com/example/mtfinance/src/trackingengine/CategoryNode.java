package com.example.mtfinance.src.trackingengine;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.Set;

/**
 * POJO pattern. Defines the relations between Categories and sub-Categories (tree structure).
 */
public class CategoryNode {

    @Embedded
    public Category category;

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "parentId"
    )

    public Set<CategoryNode> children;
}
