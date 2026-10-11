package com.ohgiraffers.restaurant.member.dto;

/* 설명. [Phase 2] 로그인 결과
 *  Phase 1 의 로그인 응답은 회원 정보뿐이었다. Phase 2 에서는 서버가 서명한 Access Token 이 함께 나간다.
 *  - accessToken : 이후 모든 요청의 Authorization 헤더에 'Bearer 토큰' 으로 붙여 보낸다
 *  - member      : 화면에 이름·권한을 보여주기 위한 회원 정보 (신원 증명에는 쓰지 않는다)
 * */
public class LoginResponse {

    private final String accessToken;
    private final MemberResponse member;

    public LoginResponse(String accessToken, MemberResponse member) {
        this.accessToken = accessToken;
        this.member = member;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public MemberResponse getMember() {
        return member;
    }
}
