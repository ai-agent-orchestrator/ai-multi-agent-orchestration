package com.ohgiraffers.restaurant.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/* 설명. 요청마다 Authorization 헤더의 Bearer 토큰을 확인해서 '누구의 요청인지'를 Spring Security 에 알려주는 필터
 *  ----------------------------------------------------------------------------------
 *  Bearer 토큰 표준(RFC 6750)에서 정한 헤더 모양
 *      Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIi...
 *  ----------------------------------------------------------------------------------
 *  이 필터가 하는 일
 *  1) 헤더가 없거나 Bearer 로 시작하지 않으면 : 아무것도 하지 않고 다음 필터로 넘긴다. (비로그인 요청)
 *  2) 토큰이 유효하면 : SecurityContext 에 인증 정보를 넣는다. 이후 컨트롤러에서 @AuthenticationPrincipal 로 꺼낼 수 있다.
 *  3) 토큰이 잘못되었으면 : 인증 정보를 넣지 않고 넘긴다. 잘못된 이유만 요청에 적어둔다.
 *  ----------------------------------------------------------------------------------
 *  이 필터는 요청을 막지 않는다. '누구인지'만 알려주고, 막을지 말지는 SecurityConfig 의 주소 규칙이 정한다.
 *  그래서 만료된 토큰을 들고 메뉴 목록(누구나 볼 수 있는 API)을 요청해도 정상 응답이 나간다.
 *  (노션 '4. JWT 인증·인가 구현' 4절)
 *  ----------------------------------------------------------------------------------
 *  OncePerRequestFilter : 요청 하나에 딱 한 번만 실행되도록 보장하는 Spring 의 필터 기본 클래스
 * */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 토큰이 잘못된 이유를 요청에 적어둘 때 쓰는 이름. JwtAuthenticationEntryPoint 가 이 이름으로 꺼내 읽는다.
    public static final String JWT_ERROR_ATTRIBUTE = "jwtError";

    // Authorization 헤더 값의 앞부분. 표준에서 정한 글자 그대로다. (뒤에 공백 한 칸 포함)
    private static final String BEARER_PREFIX = "Bearer ";

    // 토큰 검증 부품 (SecurityConfig 가 new 로 이 필터를 만들 때 넘겨준다)
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /* 설명. 이 필터를 건너뛸 요청
     *  메뉴 이미지(/uploads/**)는 누구나 받는 파일이고 토큰을 볼 필요가 없다.
     *  메뉴 목록 화면 하나에 이미지 요청이 수십 개라서, 건너뛰지 않으면 아래 로그가 이미지 요청으로 뒤덮인다.
     * */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 로그에 찍을 요청 이름. 예) GET /api/members/me
        String requestLine = request.getMethod() + " " + request.getRequestURI();
        System.out.println("[JwtAuthenticationFilter] 요청 도착 : " + requestLine);

        // Authorization 헤더 값을 꺼낸다. 없으면 null 이다.
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {

            // 1) 토큰이 없는 요청 (로그인하지 않은 사용자, 로그인 요청 자체 등)
            System.out.println("[JwtAuthenticationFilter] 토큰 없음 -> 인증 정보 없이 다음 필터로 넘긴다");

        } else {

            // 'Bearer ' 일곱 글자를 떼어낸 나머지가 토큰이다.
            String token = authorization.substring(BEARER_PREFIX.length());
            System.out.println("[JwtAuthenticationFilter] Bearer 토큰 발견 -> 검증한다");

            try {
                // 서명과 만료 시각을 검증하고, 토큰 안의 값으로 사용자 객체를 만든다. (DB 조회 없음)
                CustomUserDetails user = jwtTokenProvider.parseAccessToken(token);

                /* 설명. 2) 토큰이 유효한 요청
                 *  '인증이 끝난 사용자' 라는 표시(Authentication)를 만든다.
                 *  - principal   : 누구인지 (CustomUserDetails)
                 *  - credentials : 비밀번호 자리. 이미 토큰으로 확인했으므로 필요 없어서 null 이다.
                 *  - authorities : 권한 목록 (주소 규칙의 hasRole 이 이 값을 본다)
                 * */
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());

                // 빈 SecurityContext 를 만들어 인증 정보를 담고, 이번 요청이 처리되는 동안 쓸 수 있게 등록한다.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                System.out.println("[JwtAuthenticationFilter] 검증 성공 -> SecurityContext 에 저장 : "
                        + user.getMemberId() + " (" + user.getMemberRole() + ")");

            } catch (ExpiredJwtException e) {
                // 3-1) 만료된 토큰. 이유를 적어두면 401 응답을 만들 때 JwtAuthenticationEntryPoint 가 읽는다.
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "토큰이 만료되었습니다. 다시 로그인해주세요.");
                System.out.println("[JwtAuthenticationFilter] 만료된 토큰 -> 인증 정보 없이 다음 필터로 넘긴다");

            } catch (JwtException | IllegalArgumentException e) {
                // 3-2) 서명 불일치(위조), 형식 오류 등
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "유효하지 않은 토큰입니다.");
                System.out.println("[JwtAuthenticationFilter] 잘못된 토큰(" + e.getClass().getSimpleName()
                        + ") -> 인증 정보 없이 다음 필터로 넘긴다");
            }
        }

        // 다음 필터로 넘긴다. 뒤의 필터(주소 규칙 검사)를 통과하면 컨트롤러가 실행되고, 끝나면 여기로 돌아온다.
        filterChain.doFilter(request, response);

        // 컨트롤러(또는 401 · 403 핸들러)가 응답을 만든 뒤다. 필터는 요청이 갈 때와 응답이 올 때 두 번 지나간다.
        System.out.println("[JwtAuthenticationFilter] 응답 완료 : " + requestLine + " -> " + response.getStatus());
    }
}
