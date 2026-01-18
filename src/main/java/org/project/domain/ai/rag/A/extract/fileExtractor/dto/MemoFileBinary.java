package org.project.domain.ai.rag.A.extract.fileExtractor.dto;

public record MemoFileBinary(
        byte[] bytes,
        String fileName,
        String extension,
        long fileSize,
        String s3Key
) {}

