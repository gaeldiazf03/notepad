/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package cc.chokoka.notepad.io;

import java.util.Locale;

public final class EditorFileNameFormatter {
    private static final String DEFAULT_NAME = "note";

    private EditorFileNameFormatter() {
    }

    public static String toDefaultMarkdownName(String title) {
        String result = title == null
                ? ""
                : title.replace('\n', ' ').trim();

        if (result.length() > 20) {
            result = result.substring(0, 20).trim();
        }

        if (result.isEmpty()) {
            result = DEFAULT_NAME;
        }

        result = result.replaceAll("[\\\\/:*?\"<>|]", "_");
        while (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }

        if (result.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return result;
        }

        final int extensionIndex = result.lastIndexOf('.');
        if (extensionIndex > 0) {
            result = result.substring(0, extensionIndex);
        }

        return result + ".md";
    }
}