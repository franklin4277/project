package com.bankapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTimeUtil() {
    }

    public static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    public static String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    public static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, DATE_FORMATTER);
    }
}
