package org.project.global.util.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500;

    public List<String> chunk(String text) {

        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            int end = Math.min(text.length(), i + CHUNK_SIZE);
            chunks.add(text.substring(i, end));
        }

        return chunks;
    }
}

