package com.ohgiraffers.restaurant.common.security;

import com.ohgiraffers.restaurant.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/* 설명. 로그인이 필요한 API 를 인증 없이 요청했을 때 -> 401 Unauthorized
 *  ----------------------------------------------------------------------------------
 *  Spring Security 가 요청을 막는 곳은 컨트롤러보다 앞에 있는 필터다.
 *  그래서 여기서 생긴 거절은 @RestControllerAdvice(GlobalExceptionHandler)까지 가지 않는다.
 *  응답 모양을 다른 오류와 똑같이 맞추려면 ErrorResponse 를 여기서 직접 JSON 으로 써줘야 한다.
 *  (노션 '4. JWT 인증·인가 구현' 6절)
 *  ----------------------------------------------------------------------------------
 *  401 과 403 의 차이
 *  - 401 Unauthorized : 누구인지 모른다 (토큰이 없거나, 만료되었거나, 위조됨)       -> 이 클래스
 *  - 403 Forbidden    : 누구인지는 아는데 권한이 없다 (손님이 관리자 API 를 요청함) -> JwtAccessDeniedHandler
 * */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 자바 객체(ErrorResponse)를 JSON 문자열로 바꾸는 도구. Spring Boot 가 만들어 둔 빈을 주입받는다.
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /* 설명. 주소 규칙 검사(authenticated, hasRole)에서 '인증 정보가 없다'고 판정되면 Spring Security 가 부른다. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // JwtAuthenticationFilter 가 적어둔 이유(만료, 위조 등)가 있으면 그것을, 없으면 토큰 자체가 없는 경우다.
        Object jwtError = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
        String detail = jwtError != null ? jwtError.toString() : "로그인이 필요합니다.";

        System.out.println("[JwtAuthenticationEntryPoint] 인증 정보 없음 -> 401 응답 : "
                + request.getMethod() + " " + request.getRequestURI() + " (" + detail + ")");

        // 다른 오류 응답과 같은 모양으로 만든다.
        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00008", "인증 실패", detail);

        // 상태 코드, 본문 형식(JSON), 한글이 깨지지 않도록 문자 인코딩을 정하고 본문을 쓴다.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
