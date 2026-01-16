package org.project.domain.ai.resource;

import org.springframework.core.io.Resource;

public interface FileResourceResolver {

    Resource resolve(String s3Key);
}
