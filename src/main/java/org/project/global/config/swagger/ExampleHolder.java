package org.project.global.config.swagger;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 데이터 전달용
public class ExampleHolder {

    private Example holder;
    private String name;
    private int code;
}