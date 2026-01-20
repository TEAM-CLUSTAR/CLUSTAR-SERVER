package org.project.domain.ai.rag.G.generate;

import org.project.domain.ai.rag.F.augment.dto.RagPrompt;

public interface RagGenerator {

    String generate(RagPrompt prompt);

    String generateForPlan(RagPrompt prompt, String model, Double temperature);
}
