package com.ohgiraffers.restaurant.member.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import com.ohgiraffers.restaurant.member.dto.MemberResponse;
import com.ohgiraffers.restaurant.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "회원", description = "회원 정보")
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /* 목차. 1. 내 정보 조회 */
    /* 설명. [Phase 2] /api/members/{memberCode} 가 /api/members/me 로 바뀌었다.
     *  경로의 회원코드 대신, JwtAuthenticationFilter 가 토큰에서 꺼낸 사용자를 @AuthenticationPrincipal 로 받는다.
     * */
    @Operation(summary = "내 정보 조회", description = """
            토큰의 주인 정보를 조회한다.
            - 200 : result.member (memberCode, memberId, memberName, memberEmail, memberRole)
            - 401 : 토큰이 없거나 만료·위조됨
            """)
    @GetMapping("/me")
    public ResponseEntity<ResponseMessage> findMyInfo(@AuthenticationPrincipal CustomUserDetails user) {

        MemberResponse member = memberService.findMember(user.getMemberCode());

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "회원 정보 조회 성공", Map.of("member", member)));
    }
}
