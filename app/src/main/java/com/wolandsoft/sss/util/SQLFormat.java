package com.wolandsoft.sss.util;

import java.text.MessageFormat;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public class SQLFormat {

    public static String format(String format, String replacements, Object... args) {
        String[] parts = replacements.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            format = format.replaceAll("\\{" + part + "\\}", "{" + i + "}");
        }
        return MessageFormat.format(format, args);
    }
}
