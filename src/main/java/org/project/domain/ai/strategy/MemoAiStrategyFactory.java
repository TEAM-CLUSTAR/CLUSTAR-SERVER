package org.project.domain.ai.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MemoAiStrategyFactory {

    private final Map<MemoAiOptions, MemoAiStrategy> strategies;

    public MemoAiStrategyFactory(List<MemoAiStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(MemoAiStrategy::supports, s -> s));
    }

    public MemoAiStrategy get(MemoAiOptions type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported type"));
    }
}
