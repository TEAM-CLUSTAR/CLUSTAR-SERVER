package org.project.domain.ai.rag.A.extract.imageExtractor;

public interface ImageOcrProcessor {
    String extractText(byte[] imageBytes);
}
