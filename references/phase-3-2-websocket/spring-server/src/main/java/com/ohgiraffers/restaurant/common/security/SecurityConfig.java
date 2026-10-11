package com.ohgiraffers.restaurant.common.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/* 설명. Spring Security 설정의 본체
 *  ----------------------------------------------------------------------------------
 *  spring-boot-starter-security 를 추가하기만 하면, 이 설정이 없어도 모든 요청이 막히고
 *  로그인 화면(formLogin)과 세션이 자동으로 켜진다. 세션 기반 인증이 Spring Security 의 기본값이기 때문이다.
 *  이 클래스는 그 기본값을 '토큰 기반 + REST API' 에 맞게 바꾼다.
 *  ----------------------------------------------------------------------------------
 *  이 클래스가 만드는 빈
 *  - SecurityFilterChain   : 요청이 컨트롤러에 닿기 전에 거치는 보안 필터들의 묶음과 규칙
 *  - PasswordEncoder       : 비밀번호 암호화 · 비교 (BCrypt)
 *  - AuthenticationManager : 로그인할 때 아이디 · 비밀번호를 검사하는 처리기
 *  - CommandLineRunner     : 서버가 뜰 때 필터 체인의 필터 목록을 콘솔에 찍는다 (학습용)
 *  ----------------------------------------------------------------------------------
 *  각 설정을 쓰는 이유 : 노션 '4. JWT 인증·인가 구현' 5절
 * */
@Configuration
public class SecurityConfig {

    // 토큰 발급 · 검증 부품. JWT 필터를 만들 때 넘겨준다.
    private final JwtTokenProvider jwtTokenProvider;
    // 인증 실패(401) 응답을 만드는 부품
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    // 인가 실패(403) 응답을 만드는 부품
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /* 설명. 보안 필터 체인 조립
     *  서버가 뜰 때 한 번만 실행된다. 요청마다 실행되는 것은 여기서 조립한 필터들이다.
     * */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        System.out.println("[SecurityConfig] 보안 필터 체인 조립 시작");

        http
                /* 목차. 1. 세션 기반 인증 기능 끄기 */
                // 세션을 만들지 않는다(STATELESS). 요청마다 토큰으로 누구인지 확인하므로 서버가 기억할 필요가 없다.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 아이디 · 비밀번호를 받는 기본 로그인 화면을 끈다. 로그인은 우리가 만든 POST /api/auth/login 이 JSON 으로 받는다.
                .formLogin(AbstractHttpConfigurer::disable)
                // 브라우저 팝업으로 아이디 · 비밀번호를 받는 Basic 인증을 끈다.
                .httpBasic(AbstractHttpConfigurer::disable)
                // CSRF 보호를 끈다. CSRF 는 브라우저가 쿠키를 '자동으로' 붙여 보내는 점을 노리는 공격인데,
                // 이 프로젝트는 토큰을 쿠키가 아니라 Authorization 헤더에 JavaScript 가 직접 붙이므로 해당하지 않는다.
                .csrf(AbstractHttpConfigurer::disable)

                /* 목차. 2. CORS */
                // Security 필터는 컨트롤러보다 앞에서 동작한다. 브라우저의 사전 확인 요청(OPTIONS)에는 토큰이 없어서,
                // 이 줄이 없으면 401 로 막히고 브라우저는 CORS 오류를 띄운다. (노션 4. 7절)
                // withDefaults() 는 이미 만들어 둔 CorsConfig 의 설정을 Security 에서도 쓰게 한다.
                .cors(Customizer.withDefaults())

                /* 목차. 3. 주소별 접근 규칙 */
                /* 설명. 도메인마다 블록을 나눠 두었다. 새 API 를 만들면 자기 도메인 블록에 규칙 한 줄을 추가한다.
                 *  - 위에서부터 차례로 검사하고, 처음 맞는 규칙 하나만 적용한다. (그래서 넓은 규칙은 맨 아래에 둔다)
                 *  - permitAll()      : 토큰 없이 누구나
                 *  - authenticated()  : 토큰이 있는 사람 (로그인한 회원)
                 *  - hasRole("ADMIN") : 권한이 ROLE_ADMIN 인 사람 (hasRole 은 앞에 'ROLE_' 을 붙여서 비교한다)
                 * */
                .authorizeHttpRequests(auth -> auth

                        /* 목차. 3-1. 공통 (특정 도메인에 속하지 않는 주소) */
                        /* 설명. [Phase 3] SSE 응답이 끝날 때 Spring 은 같은 요청을 내부에서 한 번 더 처리(ASYNC 디스패치)한다.
                         *  이 두 번째 처리에서는 JWT 필터가 다시 돌지 않아 인증 정보가 비어 있다. (세션도 쓰지 않는다)
                         *  막아 두면 이미 응답을 보내는 중이라 'response is already committed' 오류가 난다.
                         *  첫 요청에서 이미 주소 규칙 검사를 통과했으므로, 내부 처리는 열어 둔다.
                         * */
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 서버 내부의 오류 처리 경로. 막으면 실제 오류(400, 404 등) 대신 401 이 보인다.
                        .requestMatchers("/error").permitAll()
                        // API 명세 (Swagger UI 화면과 명세 JSON)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 서버 상태 확인
                        .requestMatchers("/actuator/health").permitAll()
                        // 업로드된 메뉴 이미지 파일. <img> 태그는 토큰을 붙이지 않으므로 누구나 받을 수 있어야 한다.
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        /* 목차. 3-2. 회원(member) */
                        // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        // 로그인 (토큰을 받으러 오는 요청이므로 토큰이 있을 수 없다)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // 내 정보 조회
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()

                        /* 목차. 3-3. 메뉴(menu) */
                        // 메뉴 목록 · 상세 조회
                        .requestMatchers(HttpMethod.GET, "/api/menus", "/api/menus/**").permitAll()
                        // 카테고리 목록 조회
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        // 알레르기 목록 조회
                        .requestMatchers(HttpMethod.GET, "/api/allergens").permitAll()
                        // 메뉴 관리 (목록 · 등록 · 수정 · 삭제)
                        .requestMatchers("/api/admin/menus", "/api/admin/menus/**").hasRole("ADMIN")

                        /* 목차. 3-4. 장바구니(cart) */
                        // 내 장바구니 조회 · 담기 · 수량 변경 · 빼기
                        .requestMatchers("/api/members/me/cart", "/api/members/me/cart/**").authenticated()

                        /* 목차. 3-5. 주문(order) */
                        // 주문하기
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                        // 내 주문 내역 조회
                        .requestMatchers(HttpMethod.GET, "/api/members/me/orders").authenticated()
                        // 전체 주문 조회 (관리자)
                        .requestMatchers(HttpMethod.GET, "/api/admin/orders").hasRole("ADMIN")

                        /* 목차. 3-6. AI(ai)  [Phase 3] */
                        // AI 메뉴 추천 채팅. 호출할 때마다 OpenAI 요금이 나가므로 로그인한 사람만 쓴다.
                        .requestMatchers(HttpMethod.POST, "/api/ai/recommendations").authenticated()
                        /* 설명. [Phase 3-2] 관리자 메뉴 설명 초안 WebSocket
                         *  연결을 여는 HTTP 요청(핸드셰이크)에는 브라우저가 Authorization 헤더를 붙일 수 없다.
                         *  그래서 여기서는 열어 두고, 연결한 뒤 첫 메시지(auth)의 토큰으로 MenuDraftWebSocketHandler 가
                         *  관리자인지 확인한다. 인증하지 않은 연결은 핸들러가 바로 닫는다.
                         * */
                        .requestMatchers("/ws/admin/menu-draft").permitAll()

                        /* 목차. 3-7. 위 블록에 없는 나머지 요청 (맨 아래에 둔다) */
                        // 관리자 주소는 규칙을 빠뜨려도 관리자만 들어오게 한 번 더 막아 둔다.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 그 밖의 모든 요청은 로그인한 사람만
                        .anyRequest().authenticated()
                )

                /* 목차. 4. 인증 · 인가 실패 응답을 우리 오류 응답 모양(ErrorResponse)으로 */
                .exceptionHandling(exception -> exception
                        // 누구인지 모를 때 (토큰 없음, 만료, 위조) -> 401
                        .authenticationEntryPoint(authenticationEntryPoint)
                        // 누구인지는 아는데 권한이 없을 때 (손님이 관리자 API 요청) -> 403
                        .accessDeniedHandler(accessDeniedHandler)
                )

                /* 목차. 5. JWT 필터 등록 */
                /* 설명. 원래 아이디 · 비밀번호를 검사하던 필터(UsernamePasswordAuthenticationFilter) 자리 바로 앞에 끼워 넣는다.
                 *  그래서 뒤에 오는 필터들(주소 규칙 검사 등)은 이 필터가 알려준 '누구인지'를 보고 판단한다.
                 *  주의. 이 필터를 @Component 로 등록하면 Spring Boot 가 일반 서블릿 필터로도 한 번 더 등록해서
                 *       요청마다 두 번 실행된다. 그래서 빈으로 만들지 않고 여기서 new 로 직접 만든다.
                 * */
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        System.out.println("[SecurityConfig] 보안 필터 체인 조립 완료");

        // 지금까지의 설정으로 필터 체인 객체를 만들어 빈으로 등록한다.
        return http.build();
    }

    /* 목차. 6. 비밀번호 암호화 방식 */
    /* 설명. BCrypt 는 같은 비밀번호라도 매번 다른 결과가 나오는 단방향 해시다. (salt 를 섞기 때문)
     *  그래서 비교할 때는 equals() 가 아니라 passwordEncoder.matches(입력값, 저장값) 을 쓴다.
     *  회원가입(암호화)과 로그인(비교) 모두 이 빈을 쓴다.
     * */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* 목차. 7. 로그인 처리기 */
    /* 설명. AuthenticationManager 는 "아이디와 비밀번호가 맞는가"를 판정하는 Spring Security 의 로그인 처리기다.
     *  CustomUserDetailsService(회원 찾기)와 위의 PasswordEncoder(비밀번호 비교)를 Spring Security 가 자동으로 연결해 만든다.
     *  우리는 그렇게 만들어진 것을 꺼내서 빈으로 등록만 한다. MemberService 의 로그인 메서드가 이 빈을 주입받아 쓴다.
     * */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /* 목차. 8. 필터 체인 확인용 출력 (학습용) */
    /* 설명. 서버가 뜬 직후 한 번 실행되어, 요청이 거치는 보안 필터를 순서대로 찍는다.
     *  우리가 만든 JwtAuthenticationFilter 가 UsernamePasswordAuthenticationFilter 자리 근처에 들어간 것을 확인할 수 있다.
     *  formLogin 과 httpBasic 을 껐기 때문에 UsernamePasswordAuthenticationFilter 자체는 목록에 없다.
     * */
    @Bean
    public CommandLineRunner printSecurityFilters(SecurityFilterChain securityFilterChain) {
        return args -> {
            System.out.println("[SecurityConfig] 요청이 거치는 보안 필터 (위에서부터 차례로)");
            // 필터 체인에 들어 있는 필터를 순서대로 꺼내 번호와 클래스 이름을 찍는다.
            List<Filter> filters = securityFilterChain.getFilters();
            for (int i = 0; i < filters.size(); i++) {
                System.out.println("    " + (i + 1) + ". " + filters.get(i).getClass().getSimpleName());
            }
        };
    }
}
