package com.ohgiraffers.restaurant.member.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.member.dto.LoginRequest;
import com.ohgiraffers.restaurant.member.dto.LoginResponse;
import com.ohgiraffers.restaurant.member.dto.MemberResponse;
import com.ohgiraffers.restaurant.member.dto.SignupRequest;
import com.ohgiraffers.restaurant.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "인증", description = "회원가입 · 로그인")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /* 목차. 1. 회원가입 */
    @Operation(summary = "회원가입", description = """
            가입한 회원의 권한은 항상 ROLE_USER다. 비밀번호는 BCrypt로 암호화해서 저장한다. 토큰 없이 요청한다.
            - 201 : result.member (memberCode, memberId, memberName, memberEmail, memberRole)
            - 400 : 입력값 검증 실패 / 409 : 이미 사용 중인 아이디
            """)
    @SecurityRequirements   // 이 API 는 토큰이 필요 없다고 Swagger 에 표시한다
    @PostMapping("/signup")
    public ResponseEntity<ResponseMessage> signup(@Valid @RequestBody SignupRequest signupRequest) {

        MemberResponse member = memberService.signup(signupRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseMessage(HttpStatus.CREATED.value(), "회원가입 성공", Map.of("member", member)));
    }

    /* 목차. 2. 로그인 */
    /* 설명. [Phase 2] 응답에 Access Token 이 담긴다. 클라이언트는 이후 요청마다 'Authorization: Bearer 토큰' 을 붙인다. */
    @Operation(summary = "로그인", description = """
            아이디와 비밀번호가 맞으면 Access Token 을 발급한다. 토큰 없이 요청한다.
            이후 요청에는 'Authorization: Bearer {accessToken}' 헤더를 붙인다. 토큰 유효시간은 설정값(기본 1시간)이다.
            - 200 : result.accessToken (문자열), result.member (memberCode, memberId, memberName, memberEmail, memberRole)
            - 400 : 입력값 검증 실패 / 401 : 아이디 또는 비밀번호 불일치
            """)
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ResponseMessage> login(@Valid @RequestBody LoginRequest loginRequest) {

        // 여기까지 왔다면 요청이 보안 필터 체인을 통과했다는 뜻이다. (로그인 주소는 permitAll)
        System.out.println("[AuthController] 로그인 요청 : memberId=" + loginRequest.getMemberId());

        // 아이디 · 비밀번호 검사와 토큰 발급은 서비스가 한다. 실패하면 LoginFailedException -> 401
        LoginResponse login = memberService.login(loginRequest);

        // 응답의 result 에 accessToken 과 member 를 담는다.
        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "로그인 성공",
                        Map.of("accessToken", login.getAccessToken(), "member", login.getMember())));
    }
}
