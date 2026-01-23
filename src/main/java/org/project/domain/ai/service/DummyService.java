package org.project.domain.ai.service;

import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DummyService {


    public MemoAiResponse generate(MemoAiRequest request) {

        // userPrompt 길이 체크
        if (request.userPrompt().length() < 6) {

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String title = "성격과 사회 심리 핵심 요약";

            String content = """
                    ### 성격의 이해와 측정
                    성격은 개인의 특징적인 사고, 감정, 행동 방식을 의미하며, 과거 경험, 현재 동기, 희망, 공포 등에 의해 형성되는 구성개념
                    
                    *   **성격 측정 방식**
                        *   **자기보고식 질문지:** 자신의 생각·감정·행동을 직접 보고하는 방식
                            *   MMPI(미네소타 다면적 인성검사): 우울, 불안, 공격성 등 측정, 타당도 척도 포함
                            *   한계: 자기 인지의 부정확성, 사회적으로 바람직한 응답 경향
                        *   **투사검사:** 모호한 자극에 대한 반응 해석을 통한 내면 탐색
                            *   로르샤하 잉크반점, TAT, HTP
                            *   한계: 검사자 편향 가능성, 신뢰도·타당도 논란
                    
                    *   **특질적 접근**
                        *   성격을 **일관된 행동 경향(traits)**의 조합으로 파악
                        *   **요인분석:** 유사하게 반응하는 성격 형용사를 묶어 핵심 차원 도출
                        *   > **Big 5**
                            > *   **경험에 대한 개방성, 성실성, 외향성, 우호성, 신경성**
                    
                    ### 사회 영향과 사회 인지
                    사회 영향은 타인의 행동이 우리의 행동에 미치는 힘
                    
                    *   **사회 영향의 주요 동기**
                        1. **쾌락 추구**
                        2. **인정 추구**
                        3. **정확성 추구**
                    """;

            MemoAiOptions option = MemoAiOptions.SUMMARY;

            List<Long> memoIds = List.of(13L, 19L);

            String usedPrompt = "";

            return MemoAiResponse.of(
                    title,
                    content,
                    option,
                    memoIds,
                    usedPrompt
            );
        } else {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String title = "시험 대비 요약";

            String content = """
                     ### 1. 성격의 이해
    
                     *   **성격 개념 및 측정**
                         *   **성격**: 개인의 특징적인 사고, 감정, 행동 방식
                         *   **측정 방법**
                             *   **자기보고식 질문지**: MMPI 등, 자신의 생각·감정·행동 직접 보고
                             *   **투사 검사**: 로르샤하, TAT 등, 모호한 자극 반응 해석
                     *   **특질적 접근**
                         *   **핵심**: 성격은 **일관된 행동 경향(traits)**의 조합
                         *   **Big 5 모델**: 가장 널리 수용되는 성격 모델
                             *   **개방성, 성실성, 외향성, 우호성, 신경성**
                     *   **생물학적 접근**
                         *   **유전**: 성격 차이의 약 50%가 유전의 영향
                         *   **뇌와 성격**: 각성수준 차이로 외향성–내향성 설명
                             *   **BAS(행동 활성화 시스템)**: 보상 추구
                             *   **BIS(행동 억제 시스템)**: 처벌 회피
    
                     ---
    
                     ### 2. 사회적 영향 및 설득
    
                     *   **사회 영향의 정의 및 동기**
                         *   **정의**: 타인의 행동이 우리의 행동에 미치는 힘
                         *   **기본 동기**: **쾌락 추구**, **인정 추구**, **정확성 추구**
                     *   **사회 영향의 유형**
                         *   **쾌락 추구**: 보상과 처벌을 통한 행동 조절
                         *   **인정 추구**: 타인에게 수용받으려는 욕구
                             *   **규범**: 사회에서 공유되는 행동 기준 (상호성 규범, 문전박대 기법)
                             *   **동조**: 다른 사람들이 해서 따라 하는 행동 (Asch의 선분 실험)
                             *   **복종**: 권위자의 지시에 따르는 행동 (Milgram 실험)
                         *   **정확성 추구**: 옳은 믿음을 가지고자 하는 욕구
                             *   **정보적 영향**: 타인의 행동을 통해 무엇이 맞는지 배우려는 경향
                     *   **설득 및 인지 부조화**
                         *   **설득**: 의사소통으로 인한 태도나 신념 변화
                             *   **체계적 설득**: 논리·근거로 이성적 처리
                             *   **편의적 설득**: 감정·습관·간단한 판단 단서에 의존
                             *   **문간에 발 들여놓기 기법**: 작은 요구 수락 후 큰 요구도 수락 가능성 증가
                         *   **인지 부조화**: 행동과 태도 불일치 시 불편함 발생 → 일관성을 위해 태도·행동 변화 시도
    
                     ---
    
                     ### 3. 사회 인지 및 귀인
    
                     *   **사회 인지 및 고정관념**
                         *   **사회 인지**: 사람들이 타인에 대해 생각하고 추론하는 방식
                         *   **고정관념**: 범주에 근거해 타인을 추론, 자동적·빠르지만 오류 유발
                             *   **문제점**: 부정확성, 남용, 자기실현·자기지속적 성격
                             *   **자동화**: 무의식적으로 작동하여 차별적 판단 유발 (**암묵적 연합 검사 IAT**)
                     *   **행동 원인 귀인**
                         *   **귀인**: 타인의 행동 원인을 추론하는 과정
                             *   **상황 귀인**: 환경적 원인으로 판단
                             *   **성향 귀인**: 성격·태도 때문으로 판단
                         *   **대표적 편향**
                             *   **기본적 귀인 오류(대응 편향)**: 상황을 무시하고 성향으로 귀인
                             *   **행위자–관찰자 효과**: 타인=성향 귀인, 자기=상황 귀인
                             *   **귀인 판단 기준**: **일관성, 특이성, 합치성**
                    """;

            MemoAiOptions option = MemoAiOptions.SUMMARY;

            List<Long> memoIds = List.of(13L, 19L);

            String usedPrompt = "";

            return MemoAiResponse.of(
                    title,
                    content,
                    option,
                    memoIds,
                    usedPrompt
            );
        }
    }
}
