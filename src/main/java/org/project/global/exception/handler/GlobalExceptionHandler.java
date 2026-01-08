package org.project.global.exception.handler;

import org.project.global.response.ApiResponse;
import org.project.global.exception.errorcode.ErrorCode;
import org.project.global.exception.errorcode.GlobalErrorCode;
import org.project.global.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.concurrent.RejectedExecutionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 기타 모든 예외 (예상치 못한 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception e) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다."));
    }

    // 공통 예외 핸들러
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.fail(errorCode.getCode(), errorCode.getMsg());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 요청 헤더 누락 핸들러
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        GlobalErrorCode errorCode = GlobalErrorCode.REQUEST_HEADER_EMPTY;
        ApiResponse<Void> response = ApiResponse.fail(errorCode.getCode(), errorCode.getMsg());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 잘못된 HTTP 메서드 예외 핸들러
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;
        ApiResponse<Void> response = ApiResponse.fail(errorCode.getCode(), errorCode.getMsg());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 쿼리 파라미터 혹은 PathVariable 타입 예외 핸들러
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;
        ApiResponse<Void> response = ApiResponse.fail(errorCode.getCode(), errorCode.getMsg());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // 존재하지 않는 URL 요청 시 발생하는 예외
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        GlobalErrorCode errorCode = GlobalErrorCode.NOT_FOUND_URL;

        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }

    // 입력값 검증 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.NOT_VALID_EXCEPTION;

        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }

    // @Valid / @Validated 검증 실패 시 발생
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidationException(HandlerMethodValidationException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.NOT_VALID_EXCEPTION;

        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }

    // 클라이언트가 JSON body를 잘못 보냈을 때 Valid로 안잡힌 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.REQUEST_BODY_NOT_READABLE;

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }

    // DB 제약조건 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        GlobalErrorCode errorCode = GlobalErrorCode.DB_CONSTRAINT_VIOLATION;

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }

    // 비동기 요청에서 스레드 풀 크기를 넘어선 경우 예외 발생
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRejected(RejectedExecutionException e){
        GlobalErrorCode errorCode = GlobalErrorCode.ASYNC_POOL_OVERFLOW;

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMsg()));
    }
}
