package org.project.domain.ai.rag.A.extract.imageExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleVisionImageOcrProcessor implements ImageOcrProcessor {

    private final ChatClient chatClient;

    @Override
    public String extractText(byte[] imageBytes, MimeType mimeType) {

        try {
            // Media 객체 (Spring AI 규격)
            Media imageMedia = Media.builder()
                    .mimeType(mimeType)
                    .data(imageBytes)
                    .name("memo-image")
                    .build();

            // 이미지 문서화 프롬프트
            String promptText = """
                당신은 문서 분석 전문가입니다.

                아래 이미지를 보고,
                이미지에 포함된 모든 정보를 최대한 자세하고 구조적으로 텍스트로 설명하세요.

                규칙:
                - 이미지에 글자가 있다면 가능한 그대로 옮기세요
                - 표, 영수증, 문서, 메모, 슬라이드라면 문서 형태로 정리하세요
                - 제목 / 본문 / 목록 / 수치 / 날짜 / 금액 등을 명확히 구분하세요
                - 추측하지 말고, 이미지에 보이는 내용만 설명하세요
                - 불필요한 감상이나 의견은 제외하세요
                """;

            // UserMessage (텍스트 + Media)
            UserMessage userMessage = UserMessage.builder()
                    .text(promptText)
                    .media(List.of(imageMedia))
                    .build();

            // Chat 호출
            String result = chatClient
                    .prompt(new Prompt(List.of(userMessage)))
                    .call()
                    .content();

            if (result == null || result.isBlank()) {
                return null;
            }

            return result.trim();

        } catch (Exception e) {
            log.error("Image description processing failed", e);
            return null;
        }
    }
}
