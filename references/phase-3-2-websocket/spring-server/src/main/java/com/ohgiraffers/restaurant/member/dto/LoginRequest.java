package com.ohgiraffers.restaurant.member.dto;

import jakarta.validation.constraints.NotBlank;

/* 설명. 로그인 요청 본문(JSON)을 받는 DTO */
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String memberId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String memberPassword;

    public LoginRequest() {}

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberPassword() {
        return memberPassword;
    }

    public void setMemberPassword(String memberPassword) {
        this.memberPassword = memberPassword;
    }
}
