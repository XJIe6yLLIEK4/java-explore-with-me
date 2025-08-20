package ru.practicum.common.util;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private DateTimeUtil() {}

    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);
    public static final LocalDateTimeSerializer LDT_SERIALIZER = new LocalDateTimeSerializer(FORMATTER);

    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMATTER);
    }

    public static String format(LocalDateTime ldt) {
        return ldt == null ? null : ldt.format(FORMATTER);
    }
}
