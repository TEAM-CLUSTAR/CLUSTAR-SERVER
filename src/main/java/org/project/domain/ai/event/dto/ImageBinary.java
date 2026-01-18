package org.project.domain.ai.event.dto;

import java.util.Arrays;

public record ImageBinary(
        byte[] bytes,
        String s3Key,
        Long size
) {
    public ImageBinary {
        bytes = bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}

