package org.project.domain.ai.rag.G.generate;

import org.project.domain.ai.rag.F.augment.RagPrompt;

public interface RagGenerator {

    String generate(RagPrompt prompt);
}
