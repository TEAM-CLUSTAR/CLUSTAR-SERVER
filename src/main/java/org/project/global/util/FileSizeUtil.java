package org.project.global.util;

import java.text.DecimalFormat;

public final class FileSizeUtil {

    private static final double KB = 1024.0;
    private static final double MB = KB * 1024;
    private static final double GB = MB * 1024;

    private static final String FORMAT_PATTERN = "0.00";

    private FileSizeUtil() {
    }

    public static String format(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0.00KB";
        }

        DecimalFormat format = new DecimalFormat(FORMAT_PATTERN);

        if (bytes >= GB) {
            return format.format(bytes / GB) + "GB";
        }

        if (bytes >= MB) {
            return format.format(bytes / MB) + "MB";
        }

        if (bytes >= KB) {
            return format.format(bytes / KB) + "KB";
        }

        return format.format(bytes) + "B";
    }
}
