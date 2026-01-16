package org.project.domain.ai.resource;

import org.springframework.core.io.Resource;

public interface ImageResourceResolver {

    Resource resolve(String imageKey);
}
