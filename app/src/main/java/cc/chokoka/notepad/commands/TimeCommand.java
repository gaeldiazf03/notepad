/*
 * Copyright (c) 2026 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package cc.chokoka.notepad.commands;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public record TimeCommand(Mode mode) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public enum Mode {
        DATE,
        TIME,
        DATE_TIME
    }

    public static Optional<TimeCommand> parse(String command) {
        return switch (command) {
            case "/d" -> Optional.of(new TimeCommand(Mode.DATE));
            case "/t" -> Optional.of(new TimeCommand(Mode.TIME));
            case "/time" -> Optional.of(new TimeCommand(Mode.DATE_TIME));
            default -> Optional.empty();
        };
    }

    public String toText(LocalDateTime dateTime, Locale locale) {
        final String monthDate = capitalizeFirst(DATE_FORMATTER.withLocale(locale).format(dateTime),
                locale);
        final String hourMinute = TIME_FORMATTER.withLocale(locale).format(dateTime);

        return switch (mode) {
            case DATE -> '[' + monthDate + "]";
            case TIME -> '[' + hourMinute + ']';
            case DATE_TIME -> '[' + monthDate + " | " + hourMinute + "]";
        };
    }

    private static String capitalizeFirst(String input, Locale locale) {
        if (input.isEmpty()) {
            return input;
        }

        final String first = input.substring(0, 1).toUpperCase(locale);
        if (input.length() == 1) {
            return first;
        }
        return first + input.substring(1);
    }
}