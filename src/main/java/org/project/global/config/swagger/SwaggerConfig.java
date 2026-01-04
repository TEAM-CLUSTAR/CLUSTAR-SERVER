package org.project.global.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.exception.errorcode.ErrorCode;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("SOPT cluSTAR")
                        .description("SOPT cluSTAR API 명세서")
                        .version("1.0.0"));
    }

    // == 각 API에 에러 예시 추가 커스터마이저 == //
    @Bean
    public OperationCustomizer customize() {
        // Spring이 Controller의 각 메서드 스캔마다 코드 실행

        return (Operation operation, HandlerMethod handlerMethod) -> {
            // Operation: Swagger API 문서
            // handlerMethod: Controller Method

            // 1. 메서드에서 @BusinessExceptionDescription 어노테이션 찾기
            BusinessExceptionDescription businessExceptionDescription = handlerMethod.getMethodAnnotation(
                    BusinessExceptionDescription.class);

            // 2. 어노테이션 있으면 에러 응답 예시 생성 후 Swagger에 추가
            if (businessExceptionDescription != null) {
                generateErrorCodeResponseExample(operation, businessExceptionDescription.value());
            }

            return operation;
        };
    }


    // 에러 응답 예시 생성
    private void generateErrorCodeResponseExample(
            Operation operation, SwaggerResponseDescription type) {

        // 1. 스웨거 응답 섹션 가져오기
        ApiResponses responses = operation.getResponses();

        // 2. 에러 코드 목록 가져오기
        Set<ErrorCode> errorCodeList = type.getErrorCodeList();

        // 3. 각 에러 코드 ExampleHolder로 변환 및 HTTP 상태별 그룹핑
        Map<Integer, List<org.project.global.config.swagger.ExampleHolder>> statusWithExampleHolders =
                errorCodeList.stream()
                        .map(
                                errorCode -> {
                                    return org.project.global.config.swagger.ExampleHolder.builder()
                                            .holder(
                                                    getSwaggerExample(errorCode)) // ErrorCode -> Swagger Example
                                            .code(errorCode.getStatus().value()) // 404
                                            .name(errorCode.toString()) // M001
                                            .build();
                                }
                        ).collect(groupingBy(org.project.global.config.swagger.ExampleHolder::getCode));

        // 4. 스웨거에 추가
        addExamplesToResponses(responses, statusWithExampleHolders);
    }


    // == 스웨거 설정을 위한 내부 헬퍼 메서드들 == //
    // ErrorCode -> Swwagger Example 변환
    private Example getSwaggerExample(ErrorCode errorCode) {

        // 1. 에러코드로 API 응답 객체 생성
        org.project.global.response.ApiResponse<Void> errorResponse =
                org.project.global.response.ApiResponse.fail(
                        errorCode.getCode(), // M001
                        errorCode.getMsg() // 회원을 찾을 수 없습니다.
                );

        // 2. 스웨거 example 객체
        Example example = new Example();

        // 3. 스웨거에 설명 추가
        example.description(errorCode.getMsg());

        // 4, 값 추가
        example.setValue(errorResponse);

        return example;
    }

    // Swagger에 Example 추가
    private void addExamplesToResponses(
            ApiResponses responses,
            Map<Integer, List<org.project.global.config.swagger.ExampleHolder>> statusWithExampleHolders) {

        // HTTP 상태 코드별 처리
        statusWithExampleHolders.forEach(
                (status, exampleList) -> {
                    // status: 404
                    // exampleList: [ExampleHolder(MEMBER_NOT_FOUND)


                    // 1. 응답 내용 컨테이너 생성
                    Content content = new Content();

                    // 2. MediaType 생성 (JSON)
                    MediaType mediaType = new MediaType();

                    // 3. ApiResponse 생성 (공통 응답이 아닌, 스웨거의 응답 객체임!!)
                    ApiResponse apiResponse = new ApiResponse();

                    // 4. Example들 MediaType에 추가
                    exampleList.forEach(
                            exampleHolder -> {
                                mediaType.addExamples(
                                        exampleHolder.getName(), exampleHolder.getHolder());
                            });

                    // 5. 미디어 타입 추가
                    content.addMediaType("application/json", mediaType);

                    // 6. api응답에 content 추가
                    apiResponse.setDescription("");
                    apiResponse.setContent(content);

                    // 7. api응답을 스웨거 응답에 추가
                    responses.addApiResponse(status.toString(), apiResponse);
                });
    }
}