package org.project.global.util.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int MAX_CHUNK_SIZE = 300;

    public List<String> chunk(String text) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();

        String[] paragraphs = text.split("\\n{2,}");

        for (String p : paragraphs) {
            if (p.length() <= MAX_CHUNK_SIZE) {
                chunks.add(p.trim());
            } else {
                chunks.addAll(splitByLength(p));
            }
        }

        return chunks;
    }

    private List<String> splitByLength(String text) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_CHUNK_SIZE) {
            parts.add(text.substring(i, Math.min(text.length(), i + MAX_CHUNK_SIZE)));
        }
        return parts;
    }
}

