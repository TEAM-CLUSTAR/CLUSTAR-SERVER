package org.project.domain.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.tag.dto.request.TagCreateRequest;
import org.project.domain.tag.dto.request.TagUpdateRequest;
import org.project.domain.tag.dto.response.TagHierarchyResponse;
import org.project.domain.tag.dto.response.TagListResponse;
import org.project.domain.tag.dto.response.TagParentListResponse;
import org.project.domain.tag.dto.response.TagSummaryResponse;
import org.project.domain.tag.service.TagService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tag")
@Tag(name = "태그", description = "태그 관련 API")
public class TagController {

    private final TagService tagService;

    @Operation(
            summary = "[Legacy] 태그 전체 조회",
            description = """
            사용자가 생성한 모든 태그 목록을 조회합니다.
            메모에 사용된 태그와 미사용 태그를 모두 포함합니다.
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<TagListResponse>> getAllTags(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long userId = userDetails.getUserId();

        TagListResponse response =
                tagService.getAllTags(userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "태그 생성",
            description = """
            태그를 생성합니다.
            parentTagId가 있으면 하위 태그로 생성하고, 없으면 부모 태그로 생성합니다.
            """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<TagSummaryResponse>> createTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TagCreateRequest request
    ) {
        Long userId = userDetails.getUserId();

        TagSummaryResponse response = tagService.createTag(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @Operation(
            summary = "태그 수정",
            description = """
            태그 이름을 수정합니다.
            """
    )
    @PutMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagSummaryResponse>> updateTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tagId,
            @Valid @RequestBody TagUpdateRequest request
    ) {
        Long userId = userDetails.getUserId();

        TagSummaryResponse response = tagService.updateTag(userId, tagId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "태그 삭제",
            description = """
            태그를 삭제합니다.
            태그에 연결된 메모-태그 관계도 함께 정리합니다.
            """
    )
    @DeleteMapping("/{tagId}")
    public ResponseEntity<ApiResponse<String>> deleteTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tagId
    ) {
        Long userId = userDetails.getUserId();

        tagService.deleteTag(userId, tagId);

        return ResponseEntity.ok(ApiResponse.ok("태그가 삭제되었습니다."));
    }

    @Operation(
            summary = "부모 태그 최대 10개 조회",
            description = """
            사용자의 부모 태그 최대 10개를 생성일 내림차순으로 조회합니다.
            """
    )
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<TagParentListResponse>> getParentTags(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        TagParentListResponse response = tagService.getParentTags(userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "부모 태그 기반 하위 태그 조회",
            description = """
            부모 태그를 기준으로 자식 태그와 손자 태그를 계층 구조로 조회합니다.
            """
    )
    @GetMapping("/parents/{parentTagId}/children")
    public ResponseEntity<ApiResponse<TagHierarchyResponse>> getChildAndGrandChildTags(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long parentTagId
    ) {
        Long userId = userDetails.getUserId();

        TagHierarchyResponse response = tagService.getChildAndGrandChildTags(userId, parentTagId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
