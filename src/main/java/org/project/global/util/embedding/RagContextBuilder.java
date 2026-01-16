package org.project.global.util.embedding;

import org.project.domain.ai.entity.ContextEmbedding;

import java.util.List;

public interface RagContextBuilder {

    String build(List<ContextEmbedding> chunks);
}

