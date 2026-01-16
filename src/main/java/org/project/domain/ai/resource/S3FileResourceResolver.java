package org.project.domain.ai.resource;

import lombok.RequiredArgsConstructor;
import org.project.global.util.S3Util;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3FileResourceResolver implements FileResourceResolver {

    private final S3Util s3Util;

    @Override
    public Resource resolve(String s3Key) {
        byte[] fileBytes = s3Util.download(s3Key);
        return new ByteArrayResource(fileBytes);
    }
}

