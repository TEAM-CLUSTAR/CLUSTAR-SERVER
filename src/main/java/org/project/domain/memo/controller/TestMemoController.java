package org.project.domain.memo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.service.MemoServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// == 메모 생성 테스트용이므로 추후 삭제 == //
@RestController
@RequestMapping("/api/test/memos")
@RequiredArgsConstructor
@Tag(name = "Test Memo API", description = "메모 테스트용 API (개발 전용)")
public class TestMemoController {

    private final MemoServiceImpl memoService;

    @PostMapping(value = "/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드 포함 메모 생성 (테스트용)")
    public ResponseEntity<MemoResponse> createMemoWithFiles(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) List<String> labelNames,
            @RequestPart(required = false) List<MultipartFile> images,
            @RequestPart(required = false) List<MultipartFile> files
    ) {
        MemoResponse response = memoService.createMemoWithFiles(
                userId,
                title,
                content,
                labelNames,
                images,
                files
        );

        return ResponseEntity.ok(response);
    }
}