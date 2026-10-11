package com.ohgiraffers.restaurant.member.dto;

import com.ohgiraffers.restaurant.member.domain.Member;

/* 설명. 회원 정보를 응답할 때 사용하는 DTO
 *  엔티티를 그대로 내려주면 비밀번호까지 JSON에 담겨 나간다.
 *  응답 DTO에는 화면에 필요한 값만 골라 담고, 비밀번호는 절대 넣지 않는다.
 * */
public class MemberResponse {

    private int memberCode;
    private String memberId;
    private String memberName;
    private String memberEmail;
    private String memberRole;

    public MemberResponse() {}

    public MemberResponse(int memberCode, String memberId, String memberName, String memberEmail, String memberRole) {
        this.memberCode = memberCode;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.memberRole = memberRole;
    }

    /* 설명. 엔티티를 응답 DTO로 변환한다. */
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberCode(),
                member.getMemberId(),
                member.getMemberName(),
                member.getMemberEmail(),
                member.getMemberRole()
        );
    }

    public int getMemberCode() {
        return memberCode;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public String getMemberRole() {
        return memberRole;
    }
}
