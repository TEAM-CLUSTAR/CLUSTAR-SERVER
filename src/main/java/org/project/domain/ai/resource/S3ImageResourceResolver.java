package org.project.domain.ai.resource;

import lombok.RequiredArgsConstructor;
import org.project.global.util.S3Util;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3ImageResourceResolver implements ImageResourceResolver {

    private final S3Util s3Util;

    @Override
    public Resource resolve(String s3Key) {
        byte[] imageBytes = s3Util.download(s3Key);

        return new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return s3Key;
            }
        };
    }
}
