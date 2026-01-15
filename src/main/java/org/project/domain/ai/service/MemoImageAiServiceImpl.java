package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.global.util.S3Util;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoImageAiServiceImpl implements MemoImageAiService {

    private final ChatClient chatClient;
    private final S3Util s3Util;

    @Override
    public String generateImageDescription(String s3Key) {

        byte[] imageBytes = s3Util.download(s3Key);

        UserMessage userMessage = UserMessage.builder()
                .text("""
                    이 이미지를 메모 검색용으로 사용할 수 있게
                    핵심 내용 위주로 간결하게 설명해줘.
                    객체, 텍스트, 상황 위주로 설명하고
                    불필요한 감정 표현은 제외해.
                """)
                .media(List.of(
                        Media.builder()
                                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                                .data(imageBytes)
                                .build()
                ))
                .build();

        Prompt prompt = new Prompt(userMessage);

        return  chatClient
                .prompt(prompt)
                .call()
                .content();
    }
}

