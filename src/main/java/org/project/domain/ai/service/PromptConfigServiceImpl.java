package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.RagPromptConfigRequest;
import org.project.domain.ai.dto.response.RagPromptConfigResponse;
import org.project.domain.ai.strategy.MemoAiOptions;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PromptConfigServiceImpl implements PromptConfigService {

    private final ConcurrentMap<MemoAiOptions, RagPromptConfigResponse> store = new ConcurrentHashMap<>();
    private final Map<MemoAiOptions, RagPromptConfigResponse> defaults = new EnumMap<>(MemoAiOptions.class);

    public PromptConfigServiceImpl() {
        defaults.put(MemoAiOptions.SUMMARY, RagPromptConfigResponse.of("""
                너는 사용자가 남긴 메모들을 기반으로 새로운 정리 메모를 작성하는 AI다.
                - 아래 컨텍스트에 포함된 내용만 사용한다
                - 추측하거나 없는 내용을 만들어내지 않는다
                - 출력은 반드시 2줄 이상으로 작성한다
                - 첫 줄은 제목이다
                - 두 번째 줄부터는 본문이다
                - 핵심 내용만 간결하게 요약한다
                - 5줄 이내로 작성한다
                - 불필요한 설명은 제거한다
                """, null));
        defaults.put(MemoAiOptions.MERGE, RagPromptConfigResponse.of("""
                너는 사용자가 남긴 메모들을 기반으로 새로운 정리 메모를 작성하는 AI다.
                - 아래 컨텍스트에 포함된 내용만 사용한다
                - 추측하거나 없는 내용을 만들어내지 않는다
                - 출력은 반드시 2줄 이상으로 작성한다
                - 첫 줄은 제목이다
                - 두 번째 줄부터는 본문이다
                - 중복된 내용은 제거한다
                - 문맥을 자연스럽게 연결한다
                - 하나의 완성된 문서처럼 작성한다
                """, null));
        defaults.put(MemoAiOptions.STRUCTURE, RagPromptConfigResponse.of("""
                너는 사용자가 남긴 메모들을 기반으로 새로운 정리 메모를 작성하는 AI다.
                - 아래 컨텍스트에 포함된 내용만 사용한다
                - 추측하거나 없는 내용을 만들어내지 않는다
                - 출력은 반드시 2줄 이상으로 작성한다
                - 첫 줄은 제목이다
                - 두 번째 줄부터는 본문이다
                - 계층적인 아웃라인을 만든다
                - 1, 1.1, 1.1.1 형태로 표현한다
                - 본문은 최소화하고 구조 중심으로 정리한다
                """, null));
    }

    @Override
    public RagPromptConfigResponse get(MemoAiOptions option) {
        RagPromptConfigResponse stored = store.get(option);
        return stored != null ? stored : defaults.get(option);
    }

    @Override
    public RagPromptConfigResponse upsert(MemoAiOptions option, RagPromptConfigRequest request) {
        RagPromptConfigResponse updated = RagPromptConfigResponse.of(
                request.systemPrompt(),
                request.temperature()
        );
        store.put(option, updated);
        return updated;
    }
}
