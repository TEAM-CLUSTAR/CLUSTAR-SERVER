package org.project.global.util;

import java.text.DecimalFormat;

public final class FileSizeUtil {

    private static final double KB = 1024.0;
    private static final double MB = KB * 1024;
    private static final double GB = MB * 1024;

    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    private FileSizeUtil() {
    }

    public static String format(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0.00KB";
        }

        if (bytes >= GB) {
            return FORMAT.format(bytes / GB) + "GB";
        }

        if (bytes >= MB) {
            return FORMAT.format(bytes / MB) + "MB";
        }

        if (bytes >= KB) {
            return FORMAT.format(bytes / KB) + "KB";
        }

        return FORMAT.format(bytes) + "B";
    }
}
