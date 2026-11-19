package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Unit tests for Transaction creation, builder pattern, and amount validation in the tracking engine.
 */
public class TransactionTrackingEngineTest {

    private static final BigDecimal VALID_AMOUNT = BigDecimal.valueOf(1.0);
    private static final BigDecimal NEGATIVE_AMOUNT_ONE = BigDecimal.valueOf(-5.99);
    private static final BigDecimal NEGATIVE_AMOUNT_TWO = BigDecimal.valueOf(-0.01);
    private static final BigDecimal NEGATIVE_AMOUNT_THREE = BigDecimal.valueOf(-88);

    @Test
    public void transactionBuilder_ValidInputs_ConstructsTransactionWithCorrectAmount() {
        Transaction transaction = new Transaction.Builder("Lollies", VALID_AMOUNT).build();
        assertEquals(VALID_AMOUNT, transaction.getAmount());
    }

    @Test
    public void checkAmount_NegativeOrZeroAmount_ThrowsIllegalArgumentException() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> TrackingUtlis.checkAmount(NEGATIVE_AMOUNT_ONE)),
            () -> assertThrows(IllegalArgumentException.class, () -> TrackingUtlis.checkAmount(NEGATIVE_AMOUNT_TWO)),
            () -> assertThrows(IllegalArgumentException.class, () -> TrackingUtlis.checkAmount(BigDecimal.ZERO)),
            () -> assertThrows(IllegalArgumentException.class, () -> new Transaction.Builder("Negative transaction", NEGATIVE_AMOUNT_THREE).build())
        );
    }
}
