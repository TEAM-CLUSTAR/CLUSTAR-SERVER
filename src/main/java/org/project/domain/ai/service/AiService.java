package org.project.domain.ai.service;

import java.util.List;

public interface AiService {

    List<Double> generateEmbedding(String text);

    String generateInsight(String prompt);
}
