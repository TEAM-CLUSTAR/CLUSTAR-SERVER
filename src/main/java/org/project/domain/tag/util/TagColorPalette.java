package org.project.domain.tag.util;

import java.util.Collection;
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

    public static String firstAvailableColor(Collection<String> usedColors) {
        return COLORS.stream()
                .filter(color -> !usedColors.contains(color))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("사용 가능한 태그 색상이 없습니다."));
    }

    public static List<String> colors() {
        return COLORS;
    }
}
