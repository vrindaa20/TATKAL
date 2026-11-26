package com.tatkalx.utility;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Timestamp formatting helpers for booking logs, events, and virtual clock output.
 */
public final class TimeUtils {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter TIME_ONLY_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private TimeUtils() {
    }

    public static String nowTimestamp() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    public static String nowTimeWithMillis() {
        return LocalTime.now().format(TIME_ONLY_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
