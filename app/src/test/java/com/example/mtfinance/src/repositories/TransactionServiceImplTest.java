package com.example.mtfinance.src.repositories;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.mtfinance.src.repositories.roomdatabase.TransactionDao;
import com.example.mtfinance.src.trackingengine.TrackingType;
import com.example.mtfinance.src.trackingengine.Transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

public class TransactionServiceImplTest {

    @Mock
    private TransactionDao transactionDao;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionServiceImpl(transactionDao);
    }

    @Test
    public void getById_validId_returnsTransaction() {
        Transaction transaction = new Transaction.Builder("Coffee", BigDecimal.valueOf(5.0))
                .type(TrackingType.EXPENSE)
                .build();
        transaction.setTransactionId(1L);
        when(transactionDao.getById(1L)).thenReturn(transaction);

        Transaction result = transactionService.getById(1L);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Coffee", result.getName())
        );
    }

    @Test
    public void transactionExists_existingId_returnsTrue() {
        when(transactionDao.exists(1L)).thenReturn(true);
        assertTrue(transactionService.transactionExists(1L));
    }
}
