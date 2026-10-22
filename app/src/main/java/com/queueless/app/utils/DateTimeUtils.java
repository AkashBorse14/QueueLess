package com.queueless.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public static String getTodayDateString() {
        return DATE_FORMAT.format(new Date());
    }

    public static String formatTimestampToDate(long timestamp) {
        if (timestamp <= 0) return "-";
        return DISPLAY_DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatTimestampToTime(long timestamp) {
        if (timestamp <= 0) return "-";
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a sequential queue number to standard display token (e.g. 1 -> A001, 23 -> A023)
     */
    public static String formatTokenNumber(String prefix, long sequence) {
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "T";
        }
        return String.format(Locale.getDefault(), "%s%03d", prefix.toUpperCase(), sequence);
    }
}
