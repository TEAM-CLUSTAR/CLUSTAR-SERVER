package org.project.domain.ai.controller;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.service.AiServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final AiServiceImpl aiService;

    @PostMapping("/insight")
    public String insight(@RequestBody String memoText) {
        String prompt = """
        다음은 사용자가 선택한 메모들이다.

        %s

        1. 공통 주제
        2. 반복되는 문제
        3. 실행 가능한 인사이트 3가지
        형식으로 정리해줘.
        """.formatted(memoText);

        return aiService.generateInsight(prompt);
    }
}
