package org.project.global.config.chatModel;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public class YesChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        AssistantMessage message = new AssistantMessage("YES");

        Generation generation = new Generation(message);

        return new ChatResponse(List.of(generation));
    }
}
