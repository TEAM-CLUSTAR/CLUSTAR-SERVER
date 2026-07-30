package org.project.domain.tag.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TagColorPalette {

    private static final List<ColorSet> COLOR_SETS = List.of(
            new ColorSet("#D9ECFF", "#2194FF"),
            new ColorSet("#E7DEFF", "#8259FF"),
            new ColorSet("#D0F5F5", "#02A9A9"),
            new ColorSet("#FFDBDC", "#E63639"),
            new ColorSet("#FFD9C6", "#FF6200"),
            new ColorSet("#FFEFC7", "#F99E00"),
            new ColorSet("#D1DEFF", "#3A50E0"),
            new ColorSet("#D2FFD5", "#02B50E"),
            new ColorSet("#EFD3FF", "#D93FDB"),
            new ColorSet("#FFE3EC", "#FF4E89")
    );

    private TagColorPalette() {
    }

    public static ColorSet randomColorSet() {
        return COLOR_SETS.get(ThreadLocalRandom.current().nextInt(COLOR_SETS.size()));
    }

    public static String randomColor() {
        return randomColorSet().backgroundColorHex();
    }

    public static String textColorOf(String backgroundColorHex) {
        return COLOR_SETS.stream()
                .filter(colorSet -> colorSet.backgroundColorHex().equals(backgroundColorHex))
                .map(ColorSet::textColorHex)
                .findFirst()
                .orElse(COLOR_SETS.get(0).textColorHex());
    }

    public static List<ColorSet> colorSets() {
        return COLOR_SETS;
    }

    public static List<String> backgroundColors() {
        return COLOR_SETS.stream()
                .map(ColorSet::backgroundColorHex)
                .toList();
    }

    public static List<String> textColors() {
        return COLOR_SETS.stream()
                .map(ColorSet::textColorHex)
                .toList();
    }

    public record ColorSet(
            String backgroundColorHex,
            String textColorHex
    ) {
    }
}
