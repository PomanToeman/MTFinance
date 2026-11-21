package com.example.mtfinance.src.trackingengine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HeaderAliasTest {

    private static final String PAYEE_HEADER = "payee";
    private static final String MEMO_HEADER = "memo";
    private static final String TOTAL_HEADER = "total";
    private static final String POSTING_DATE_HEADER = "posting date";
    private static final String ENTRY_TYPE_HEADER = "entry type";
    private static final String UNKNOWN_HEADER = "unrecognized_column";

    @Test
    public void matches_validNameAlias_returnsTrue() {
        assertTrue(HeaderAlias.NAME.matches(PAYEE_HEADER));
    }

    @Test
    public void matches_validDescriptionAlias_returnsTrue() {
        assertTrue(HeaderAlias.DESCRIPTION.matches(MEMO_HEADER));
    }

    @Test
    public void matches_validAmountAlias_returnsTrue() {
        assertTrue(HeaderAlias.AMOUNT.matches(TOTAL_HEADER));
    }

    @Test
    public void matches_validDateAlias_returnsTrue() {
        assertTrue(HeaderAlias.DATE.matches(POSTING_DATE_HEADER));
    }

    @Test
    public void matches_validTypeAlias_returnsTrue() {
        assertTrue(HeaderAlias.TYPE.matches(ENTRY_TYPE_HEADER));
    }

    @Test
    public void matches_unknownHeader_returnsFalse() {
        assertFalse(HeaderAlias.NAME.matches(UNKNOWN_HEADER));
    }

    @Test
    public void findMatch_knownHeaderAliases_returnsCorrectEnum() {
        assertAll(
            () -> assertEquals(HeaderAlias.NAME, HeaderAlias.findMatch(PAYEE_HEADER)),
            () -> assertEquals(HeaderAlias.DESCRIPTION, HeaderAlias.findMatch(MEMO_HEADER)),
            () -> assertEquals(HeaderAlias.AMOUNT, HeaderAlias.findMatch(TOTAL_HEADER)),
            () -> assertEquals(HeaderAlias.DATE, HeaderAlias.findMatch(POSTING_DATE_HEADER)),
            () -> assertEquals(HeaderAlias.TYPE, HeaderAlias.findMatch(ENTRY_TYPE_HEADER)),
            () -> assertNull(HeaderAlias.findMatch(UNKNOWN_HEADER))
        );
    }
}
