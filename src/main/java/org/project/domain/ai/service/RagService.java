package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public String ask(String question) {

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(3)
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .system("You are a helpful assistant. Use the following context to answer.")
                .user(context + "\n\nQuestion: " + question)
                .call()
                .content();
    }
}
