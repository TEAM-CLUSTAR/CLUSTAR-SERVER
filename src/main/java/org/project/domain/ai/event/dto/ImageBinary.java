package org.project.domain.ai.event.dto;

public record ImageBinary(
        byte[] bytes,
        String s3Key,
        Long size
) {}

