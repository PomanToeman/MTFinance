package com.example.mtfinance.src.roomdatabase;

import androidx.room.TypeConverter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class Converters {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @TypeConverter
    public static BigDecimal fromString(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @TypeConverter
    public static String fromTransactionIdSet(Set<Long> transactionIds) {
        if (transactionIds == null) return "";

        StringBuilder sb = new StringBuilder();
        for (Long id : transactionIds) {
            sb.append(id).append(",");
        }
        return sb.toString();

    }

    @TypeConverter
    public static Set<Long> toTransactionIdsSet(String value) {
        Set<Long> transactionIds = new HashSet<>();
        if (value == null || value.isEmpty()) return transactionIds; // empty set
        String[] ids = value.split(",");
        for (String id : ids) {
            transactionIds.add(Long.parseLong(id));
        }
        return transactionIds;
    }


    @TypeConverter
    public static String bigDecimalToString(BigDecimal bigDecimal) {
        return bigDecimal == null ? null : bigDecimal.toString();
    }

    @TypeConverter
    public static LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value, formatter);
    }

    @TypeConverter
    public static String dateToTimestamp(LocalDateTime date) {
        return date == null ? null : date.format(formatter);
    }
}
