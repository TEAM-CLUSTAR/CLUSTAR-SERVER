package org.project.domain.ai.event.dto;

import org.springframework.util.MimeType;

import java.util.Arrays;

public record ImageBinary(
        byte[] bytes,
        String s3Key,
        Long size,
        MimeType mimeType
) {
    public ImageBinary {
        bytes = bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}
