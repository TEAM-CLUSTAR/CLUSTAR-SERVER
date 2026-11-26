package org.project.domain.review.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.support.FindingDrafts;
import org.project.domain.review.support.ValidFindingDraft;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

// 임시: 실제 Gemini로 분석 프롬프트 품질을 확인하는 수동 실행용(REVIEW_GEMINI_SMOKE=true일 때만 실행)
// 메모마다 '꼭 잡아야 할 빈틈'을 심어 두고, 결과를 build/review-smoke/{REVIEW_EVAL_LABEL}.json에 남긴다
@EnabledIfEnvironmentVariable(named = "REVIEW_GEMINI_SMOKE", matches = "true")
class ReviewGeminiSmokeTest {

    private static final String MODEL = "gemini-2.5-flash";

    record EvalMemo(String key, String title, String content, List<String> mustCatch) {
    }

    private static final List<EvalMemo> MEMOS = List.of(
            new EvalMemo("book", "전공책 중고거래 기획서", """
                    1. 배경
                    학기가 끝나면 졸업생과 고학년이 두고 간 전공책이 그대로 버려진다. 새 학기마다 같은 책을 비싸게 새로 사는 학생도 많다. 학교 커뮤니티에 중고 거래 글이 올라오지만 거래 시간을 맞추기 어렵고 사기 걱정도 있다.

                    2. 타깃
                    전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
                    판매자는 졸업 예정자와 3~4학년이다.

                    3. 거래 방식
                    거래는 택배 대신 학생회관 사물함을 통한 비대면 방식으로 진행한다.
                    가격은 판매자가 정가의 30~50% 안에서 정한다.
                    판매자가 책을 사물함에 넣으면 거래가 확정된다.

                    4. 운영
                    학생회가 사물함 관리를 맡는다.
                    사물함 비밀번호는 거래가 확정되면 구매자에게 문자로 보낸다.
                    수수료는 받지 않고 학생회 예산으로 운영한다.""",
                    List.of(
                            "돈을 언제 어떻게 주고받는지(결제)가 거래 흐름에 없음",
                            "구매자가 책을 보기 전에 거래가 확정됨 → 배경의 '사기 걱정'과 충돌",
                            "책이 나오는 시점(학기 말)과 필요한 시점(새 학기)이 달라 그 사이 보관 문제"
                    )),
            new EvalMemo("lunch", "사내 점심 도시락 공동구매 기획", """
                    ## 문제
                    회사 근처 식당은 점심시간마다 줄이 길어서 직원들이 30분 넘게 기다린다. 점심시간이 1시간이라 줄을 서고 나면 밥을 급하게 먹어야 하고, 오후 회의에 늦는 사람도 생긴다.

                    ## 해결 방법
                    전날 오후 3시까지 사내 메신저 봇으로 도시락 주문을 받는다.
                    도시락은 근처 반찬가게 한 곳과 계약해 1인분 8,000원에 공급받는다.
                    메뉴는 반찬가게가 정한 오늘의 도시락 한 가지로 한다.
                    주문이 20개 미만이면 그날은 공동구매를 진행하지 않는다.

                    ## 운영
                    총무팀 막내가 매일 12시에 1층 로비에서 도시락을 받아 나눠 준다.
                    결제는 월말에 급여에서 한 번에 차감한다.
                    첫 달은 개발팀만 대상으로 시범 운영하고, 반응이 좋으면 전사로 넓힌다.""",
                    List.of(
                            "20개 미만으로 취소될 때 주문한 사람에게 언제 어떻게 알리는지",
                            "총무팀 막내 한 명에게 매일 배부 업무가 몰림(맡는 사람의 부담)",
                            "급여 차감을 하려면 본인 동의·급여 담당 부서가 필요함",
                            "'반응이 좋으면'의 판단 기준이 없음"
                    )),
            new EvalMemo("study", "알고리즘 스터디 운영안", """
                    목표
                    3개월 안에 참여자 전원이 기업 코딩테스트 2솔 이상을 안정적으로 하는 것.

                    진행
                    매주 토요일 오전 10시에 학교 스터디룸에서 오프라인으로 모인다.
                    스터디장이 매주 문제 5개를 골라 금요일까지 단톡방에 올린다.
                    모임에서는 각자 풀이를 10분씩 발표하고 서로 코드 리뷰를 한다.

                    규칙
                    보증금은 2만 원이고, 결석하면 한 번에 5천 원씩 차감한다.
                    결석이 3번 쌓이면 스터디에서 나가야 한다.
                    인원은 최대 8명으로 하고, 빈자리가 생기면 대기자에게 먼저 연락한다.
                    문제는 백준 골드 이상만 고른다.""",
                    List.of(
                            "차감한 보증금을 어디에 쓰는지, 끝나면 돌려주는지",
                            "아파서·시험 때문에 빠지는 경우도 결석 3번에 똑같이 세는지(예외)",
                            "스터디장 한 명이 매주 문제 선정을 맡는 부담",
                            "'2솔 이상' 목표와 '골드 이상' 난이도·주 5문제 분량이 맞는지 기준",
                            "8명 × 10분 발표면 80분 + 리뷰 — 모임 시간 안에 되는지"
                    )),
            new EvalMemo("notes", "운영체제 3주차 수업 정리", """
                    프로세스와 스레드
                    프로세스는 실행 중인 프로그램으로, 자기만의 메모리 공간(코드, 데이터, 힙, 스택)을 가진다.
                    스레드는 프로세스 안에서 실행되는 흐름이고, 같은 프로세스의 스레드끼리는 코드, 데이터, 힙을 공유하고 스택만 따로 가진다.
                    그래서 스레드 사이의 통신은 프로세스 사이의 통신보다 가볍지만, 공유 자원에 동시에 접근하면 경쟁 상태가 생길 수 있다.

                    컨텍스트 스위칭
                    CPU가 실행 중인 작업을 바꿀 때 지금 상태를 PCB에 저장하고 다음 작업의 상태를 불러온다.
                    스레드 전환은 메모리 공간을 바꾸지 않아도 되므로 프로세스 전환보다 비용이 적다.

                    다음 주 예고
                    동기화 도구인 뮤텍스와 세마포어를 배운다. 과제는 생산자-소비자 문제를 세마포어로 구현하는 것이고, 제출은 다음 주 금요일 자정까지다.""",
                    List.of(
                            "(결정이 거의 없는 정리 노트) — 억지 카드가 적어야 좋음"
                    ))
    );

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

        String only = System.getenv("REVIEW_EVAL_ONLY");
        List<Map<String, Object>> report = new ArrayList<>();
        for (EvalMemo memo : MEMOS) {
            if (only != null && !only.isBlank() && !only.contains(memo.key())) {
                continue;
            }
            report.add(run(client, memo));
        }

        String label = System.getenv().getOrDefault("REVIEW_EVAL_LABEL", "result");
        Path out = Path.of("build/review-smoke/" + label + ".json");
        Files.createDirectories(out.getParent());
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out.toFile(), report);
    }

    private Map<String, Object> run(ReviewAiClient client, EvalMemo memo) {
        long startedAt = System.currentTimeMillis();
        AnalysisResult result = client.analyze(0L, memo.title(), memo.content(), List.of(), List.of());
        long latency = System.currentTimeMillis() - startedAt;
        List<ValidFindingDraft> kept = FindingDrafts.sanitize(memo.content(), result.findings(), 10);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", memo.key());
        row.put("title", memo.title());
        row.put("latencyMs", latency);
        row.put("mustCatch", memo.mustCatch());
        row.put("rawCount", result.findings().size());
        row.put("raw", result);
        row.put("kept", kept);
        return row;
    }
}
