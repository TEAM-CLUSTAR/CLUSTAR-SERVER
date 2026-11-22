package org.project.domain.review.controller;

import org.project.global.exception.domainException.ReviewLengthException;
import org.project.global.exception.domainException.ReviewLengthException.LengthDetail;
import org.project.global.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 검토 API 전용 예외 응답. 공통 핸들러(GlobalExceptionHandler)는 고정 문구만 내려주므로,
// 글자 수 에러는 여기서 먼저 잡아 실제 글자 수와 기준을 함께 내려준다(공통 핸들러는 수정하지 않음)
@RestControllerAdvice(assignableTypes = ReviewController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewLengthException.class)
    public ResponseEntity<ApiResponse<LengthDetail>> handleLength(ReviewLengthException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode().getCode(), e.getDetail(), e.getMessage()));
    }
}
