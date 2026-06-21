package org.project.domain.label.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LabelColorPalette {

    private static final List<String> COLORS = List.of(
            "#ABDEE6", "#CBAACB", "#FFFFB5", "#FFCCB6", "#F3B0C3",
            "#C6DBDA", "#FEE1E8", "#FED7C3", "#F6EAC2", "#ECD5E3",
            "#FF968A", "#FFAEA5", "#FFC5BF", "#FFD8BE", "#FFC8A2",
            "#D4F0F0", "#8FCACA", "#CCE2CB", "#B6CFB6", "#97C1A9",
            "#FCB9AA", "#FFDBCC", "#ECEAE4", "#A2E1DB", "#55CBCD"
    );

    private LabelColorPalette() {
    }

    public static String randomColor() {
        return COLORS.get(ThreadLocalRandom.current().nextInt(COLORS.size()));
    }

    public static List<String> colors() {
        return COLORS;
    }
}
