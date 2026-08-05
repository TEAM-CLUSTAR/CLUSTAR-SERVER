package org.project.domain.tag.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TagColorPalette {

    private static final List<String> COLORS = List.of(
            "light-blue",
            "purple",
            "cyan",
            "red",
            "orange",
            "yellow",
            "blue",
            "green",
            "magenta",
            "pink"
    );

    private TagColorPalette() {
    }

    public static String randomColor() {
        return COLORS.get(ThreadLocalRandom.current().nextInt(COLORS.size()));
    }

    public static String colorOf(String colorHex) {
        if (colorHex == null) {
            return COLORS.get(0);
        }

        return switch (colorHex) {
            case "#D9ECFF" -> "light-blue";
            case "#E7DEFF" -> "purple";
            case "#D0F5F5" -> "cyan";
            case "#FFDBDC" -> "red";
            case "#FFD9C6" -> "orange";
            case "#FFEFC7" -> "yellow";
            case "#D1DEFF" -> "blue";
            case "#D2FFD5" -> "green";
            case "#EFD3FF" -> "magenta";
            case "#FFE3EC" -> "pink";
            default -> COLORS.get(0);
        };
    }

    public static List<String> colors() {
        return COLORS;
    }
}
