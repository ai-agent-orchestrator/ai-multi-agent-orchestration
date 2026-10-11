package com.ohgiraffers.restaurant.common.security;

import com.ohgiraffers.restaurant.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/* 설명. 로그인은 했지만 권한이 없는 API 를 요청했을 때 -> 403 Forbidden
 *  예) ROLE_USER 인 손님이 /api/admin/** 을 요청한 경우
 *  응답 모양을 ErrorResponse 로 맞추는 이유는 JwtAuthenticationEntryPoint 와 같다.
 * */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    // 자바 객체(ErrorResponse)를 JSON 문자열로 바꾸는 도구
    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /* 설명. 주소 규칙 검사(hasRole)에서 '인증은 됐지만 권한이 모자라다'고 판정되면 Spring Security 가 부른다. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // JwtAuthenticationFilter 가 SecurityContext 에 넣어 둔 인증 정보. 누가 막혔는지 로그로 남긴다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("[JwtAccessDeniedHandler] 권한 부족 -> 403 응답 : "
                + request.getMethod() + " " + request.getRequestURI()
                + " (요청한 사람 : " + authentication.getName() + ", 권한 : " + authentication.getAuthorities() + ")");

        // 다른 오류 응답과 같은 모양으로 만든다.
        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00009", "권한 없음", "이 요청을 처리할 권한이 없습니다.");

        // 상태 코드, 본문 형식(JSON), 문자 인코딩을 정하고 본문을 쓴다.
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
