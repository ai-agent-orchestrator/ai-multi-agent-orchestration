package com.ohgiraffers.restaurant.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/* 설명. [Phase 2] Swagger UI 에서 토큰을 넣고 요청해볼 수 있게 하는 설정
 *  Swagger UI 오른쪽 위에 'Authorize' 버튼이 생긴다. 로그인 응답의 accessToken 을 붙여넣으면
 *  이후 Swagger UI 에서 보내는 모든 요청에 'Authorization: Bearer 토큰' 헤더가 붙는다.
 *  (토큰 앞에 'Bearer ' 를 직접 쓰지 않는다. scheme = "bearer" 가 알아서 붙인다)
 * */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "오지라퍼 레스토랑 API", version = "phase-3-2"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
