package org.project.global.util.embedding;

import org.project.domain.ai.entity.ContextEmbedding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultRagContextBuilder implements RagContextBuilder {

    private static final int MAX_CONTEXT_LENGTH = 3000;

    @Override
    public String build(List<ContextEmbedding> chunks) {

        StringBuilder sb = new StringBuilder();

        for (ContextEmbedding chunk : chunks) {

            sb.append("[")
                    .append(chunk.getContextType())
                    .append(" ")
                    .append(chunk.getContextId())
                    .append(" - chunk ")
                    .append(chunk.getChunkIndex())
                    .append("]\n");

            sb.append(chunk.getSourcePreview())
                    .append("\n\n");

            if (sb.length() > MAX_CONTEXT_LENGTH) {
                break;
            }
        }

        return sb.toString();
    }
}

