package com.example.mtfinance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.TrackingUtlis;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.Test;

import java.math.BigDecimal;

public class TrackingEngineUnitTests {

    private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(0.1);
    private static final BigDecimal VALID_AMOUNT = BigDecimal.valueOf(1.0);
    private static final BigDecimal NEGATIVE_AMOUNT_ONE = BigDecimal.valueOf(-5.99);
    private static final BigDecimal NEGATIVE_AMOUNT_TWO = BigDecimal.valueOf(-0.01);
    private static final BigDecimal NEGATIVE_AMOUNT_THREE = BigDecimal.valueOf(-88);

    @Test
    public void transactionBuilder_ValidInputs_ConstructsTransactionWithCorrectAmount() {
        // Arrange & Act
        Transaction transaction = new Transaction.Builder("Lollies", VALID_AMOUNT).build();

        // Assert
        assertEquals(VALID_AMOUNT, transaction.getAmount());
    }

    @Test
    public void checkAmount_NegativeOrZeroAmount_ThrowsIllegalArgumentException() {
        // Arrange, Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(NEGATIVE_AMOUNT_ONE));

        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(NEGATIVE_AMOUNT_TWO));

        assertThrows(IllegalArgumentException.class,
                () -> TrackingUtlis.checkAmount(BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class,
                () -> new Transaction.Builder("Negative transaction", NEGATIVE_AMOUNT_THREE).build());
    }

    @Test
    public void categoryHierarchy_ParentChildRelationshipAndCyclePrevention_SetsParentAndValidatesDescendant() {
        // Arrange
        Category categoryOne = new Category("Subscriptions", "Test 3", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category categoryTwo = new Category("Netflix", "Sub category of subscriptions", DEFAULT_BUDGET, TrackingType.EXPENSE);

        // Act
        categoryTwo.setParent(categoryOne);
        categoryOne.setParent(categoryTwo); // Cycle prevention check

        // Assert
        assertEquals(categoryOne, categoryTwo.getParent());
        assertNotEquals(categoryTwo, categoryOne.getParent());
        assertFalse(categoryOne.isDescendantOf(categoryTwo));
        assertTrue(categoryTwo.isDescendantOf(categoryOne));
    }

    @Test
    public void categoryHierarchy_BasicParentChild_SetsParentSuccessfully() {
        // Arrange
        Category parent = new Category("Subscriptions", "Main category", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category child = new Category("Netflix", "Streaming sub", DEFAULT_BUDGET, TrackingType.EXPENSE);

        // Act
        child.setParent(parent);

        // Assert
        assertEquals(parent, child.getParent());
        assertTrue(child.isDescendantOf(parent));
        assertFalse(parent.isDescendantOf(child));
    }

    @Test
    public void categoryHierarchy_DeepHierarchy_ValidatesMultiLevelDescendants() {
        // Arrange
        Category root = new Category("Root", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level1 = new Category("Level 1", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level2 = new Category("Level 2", "", DEFAULT_BUDGET, TrackingType.EXPENSE);
        Category level3 = new Category("Level 3", "", DEFAULT_BUDGET, TrackingType.EXPENSE);

        // Act
        level1.setParent(root);
        level2.setParent(level1);
        level3.setParent(level2);

        // Assert
        assertTrue(level3.isDescendantOf(root));
        assertTrue(level3.isDescendantOf(level1));
        assertTrue(level2.isDescendantOf(root));
        assertFalse(root.isDescendantOf(level3));
    }
}
