package com.readverse.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeUtils() {
    }

    public static String nowText() {
        return LocalDateTime.now().format(DATE_TIME);
    }
}
