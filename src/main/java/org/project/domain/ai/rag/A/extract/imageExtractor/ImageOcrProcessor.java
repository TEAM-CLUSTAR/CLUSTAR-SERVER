package org.project.domain.ai.rag.A.extract.imageExtractor;

import org.springframework.util.MimeType;

public interface ImageOcrProcessor {
    String extractText(byte[] imageBytes, MimeType mimeType);
}
