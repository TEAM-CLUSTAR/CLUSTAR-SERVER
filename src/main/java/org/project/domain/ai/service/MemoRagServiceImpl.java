package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.RagMemoCreateRequest;
import org.project.domain.ai.dto.response.RagContextChunkResponse;
import org.project.domain.ai.dto.response.RagMemoCreateResponse;
import org.project.domain.ai.strategy.MemoAiOptions;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoRagServiceImpl implements MemoRagService {

    private static final int DEFAULT_TOP_K = 6;
    private static final String RAG_SOURCE = "RAG";

    private final ChatClient chatClient;
    private final ContextEmbeddingSearchService searchService;
    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final ContextEmbeddingService embeddingService;

    @Override
    @Transactional
    public RagMemoCreateResponse createRagMemo(Long userId, RagMemoCreateRequest request) {
        int topK = request.topK() != null ? request.topK() : DEFAULT_TOP_K;

        // 1) 질의 텍스트 → 유사 컨텍스트 검색
        List<RagContextChunkResponse> contexts = searchService.searchTopK(
                userId,
                request.userPrompt(),
                request.memoIds(),
                topK
        );

        if (contexts.isEmpty()) {
            throw new AiException(AiErrorCode.RAG_CONTEXT_NOT_FOUND);
        }

        // 2) 검색된 컨텍스트를 기반으로 정리 메모 생성
        String contextText = contexts.stream()
                .map(RagContextChunkResponse::chunkText)
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("");

        RagGeneratedMemo result = generateMemoFromContext(
                request.userPrompt(),
                contextText,
                request.option()
        );

        // 3) 새 메모 저장 (AI 생성 플래그 포함)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        String title = resolveTitle(result.title());
        Memo memo = Memo.createAiMemo(title, result.content(), user, RAG_SOURCE);
        Memo savedMemo = memoRepository.save(memo);

        // 4) 새 메모도 임베딩 저장
        embeddingService.saveMemoEmbedding(userId, savedMemo.getId(), savedMemo.getContent());

        return RagMemoCreateResponse.of(savedMemo.getId(), savedMemo.getTitle(), savedMemo.getContent(), contexts.size());
    }

    private String resolveTitle(String title) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return "RAG 정리 메모 " + now;
    }

    private RagGeneratedMemo generateMemoFromContext(
            String userPrompt,
            String contextText,
            MemoAiOptions option
    ) {
        String system = buildSystemPrompt(option);

        String user = """
                사용자 요청:
                %s

                컨텍스트:
                %s
                """.formatted(userPrompt, contextText);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(system),
                        new UserMessage(user)
                )
        );

        String raw = chatClient.prompt(prompt).call().content();
        return parseGeneratedMemo(raw);
    }

    private String buildSystemPrompt(MemoAiOptions option) {
        String behavior = switch (option) {
            case SUMMARY -> """
                    - 핵심 내용만 간결하게 요약한다
                    - 5줄 이내로 작성한다
                    - 불필요한 설명은 제거한다
                    """;
            case MERGE -> """
                    - 중복된 내용은 제거한다
                    - 문맥을 자연스럽게 연결한다
                    - 하나의 완성된 문서처럼 작성한다
                    """;
            case STRUCTURE -> """
                    - 계층적인 아웃라인을 만든다
                    - 1, 1.1, 1.1.1 형태로 표현한다
                    - 본문은 최소화하고 구조 중심으로 정리한다
                    """;
        };

        return """
                너는 사용자가 남긴 메모들을 기반으로 새로운 정리 메모를 작성하는 AI다.
                - 아래 컨텍스트에 포함된 내용만 사용한다
                - 추측하거나 없는 내용을 만들어내지 않는다
                - 출력은 반드시 2줄 이상으로 작성한다
                - 첫 줄은 제목이다
                - 두 번째 줄부터는 본문이다
                %s
                """.formatted(behavior);
    }

    /**
     * 첫 줄을 title, 이후 줄을 content로 파싱한다.
     */
    private RagGeneratedMemo parseGeneratedMemo(String raw) {
        if (raw == null) {
            return new RagGeneratedMemo(null, "");
        }

        String trimmed = raw.trim();
        int newlineIndex = trimmed.indexOf('\n');
        if (newlineIndex == -1) {
            return new RagGeneratedMemo(trimmed, "");
        }

        String title = trimmed.substring(0, newlineIndex).trim();
        String content = trimmed.substring(newlineIndex + 1).trim();
        return new RagGeneratedMemo(title, content);
    }

    private record RagGeneratedMemo(String title, String content) {
    }
}
