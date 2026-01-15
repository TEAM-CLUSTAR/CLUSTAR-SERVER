package org.project.domain.ai.controller;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RagTestController {

    private final RagService ragService;

    @GetMapping("/api/ai/rag-test")
    public String ragTest(@RequestParam String q) {
        return ragService.ask(q);
    }
}
