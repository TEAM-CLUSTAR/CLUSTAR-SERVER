package org.project.domain.review.ai;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.FindingDraft;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.support.FindingDrafts;
import org.project.domain.review.support.ValidFindingDraft;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;

// 임시: 실제 Gemini로 분석 프롬프트를 확인하는 수동 실행용(REVIEW_GEMINI_SMOKE=true일 때만 실행)
@EnabledIfEnvironmentVariable(named = "REVIEW_GEMINI_SMOKE", matches = "true")
class ReviewGeminiSmokeTest {

    private static final String MODEL = "gemini-2.5-flash";

    private static final String BOOK_TITLE = "전공책 중고거래 기획서";
    private static final String BOOK_CONTENT = """
            1. 배경
            학기가 끝나면 졸업생과 고학년이 두고 간 전공책이 그대로 버려진다. 새 학기마다 같은 책을 비싸게 새로 사는 학생도 많다.

            2. 타깃
            전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
            판매자는 졸업 예정자와 3~4학년이다.

            3. 거래 방식
            거래는 택배 대신 학생회관 사물함을 통한 비대면 방식으로 진행한다.
            가격은 판매자가 정가의 30~50% 안에서 정한다.

            4. 운영
            학생회가 사물함 관리를 맡는다.
            사물함 비밀번호는 거래가 확정되면 구매자에게 문자로 보낸다.
            """;

    private static final String LUNCH_TITLE = "사내 점심 도시락 공동구매 기획";
    private static final String LUNCH_CONTENT = """
            ## 문제
            회사 근처 식당은 점심시간마다 줄이 길어서 직원들이 30분 넘게 기다린다.

            ## 해결 방법
            전날 오후 3시까지 사내 메신저 봇으로 도시락 주문을 받는다.
            도시락은 근처 반찬가게 한 곳과 계약해 1인분 8,000원에 공급받는다.
            주문이 20개 미만이면 그날은 공동구매를 진행하지 않는다.

            ## 운영
            총무팀 막내가 매일 12시에 1층 로비에서 도시락을 받아 나눠 준다.
            결제는 월말에 급여에서 한 번에 차감한다.
            """;

    @Test
    void analyzeRealDocuments() throws Exception {
        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(Client.builder().apiKey(System.getenv("GOOGLE_AI_API_KEY")).build())
                .defaultOptions(GoogleGenAiChatOptions.builder().model(MODEL).build())
                .build();

        ReviewProperties properties = new ReviewProperties();
        properties.setMinContentLength(300);
        properties.setMaxFindings(10);
        properties.getAi().setModel(MODEL);
        properties.getAi().setAnalysisTemperature(0.2);

        ReviewAiClient client = new ReviewAiClient(chatModel, properties, mock(ReviewAiCallLogWriter.class));

        StringBuilder report = new StringBuilder();
        run(client, BOOK_TITLE, BOOK_CONTENT, report);
        run(client, LUNCH_TITLE, LUNCH_CONTENT, report);

        Path out = Path.of("build/review-smoke/result.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString());
    }

    private void run(ReviewAiClient client, String title, String content, StringBuilder report) {
        long startedAt = System.currentTimeMillis();
        AnalysisResult result = client.analyze(0L, title, content, List.of());
        long latency = System.currentTimeMillis() - startedAt;
        List<ValidFindingDraft> kept = FindingDrafts.sanitize(content, result.findings(), 10);

        report.append("==== ").append(title).append(" (").append(latency).append("ms)\n");
        report.append("AI가 준 빈틈 ").append(result.findings().size())
                .append("개 → 검증 통과 ").append(kept.size()).append("개\n\n");
        for (FindingDraft draft : result.findings()) {
            boolean ok = kept.stream().anyMatch(k -> k.title().equals(draft.title() == null ? "" : draft.title().trim()));
            report.append(ok ? "[통과] " : "[버림] ").append(draft.category()).append(" | ").append(draft.sectionLabel()).append('\n')
                    .append("  문장: ").append(draft.anchorText()).append('\n')
                    .append("  제목: ").append(draft.title()).append('\n')
                    .append("  짚은 곳: ").append(draft.reason()).append('\n')
                    .append("  근거: ").append(draft.evidence()).append('\n')
                    .append("  질문: ").append(draft.question()).append("\n\n");
        }
    }
}
