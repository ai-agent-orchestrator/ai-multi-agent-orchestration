package com.ohgiraffers.restaurant.member.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_member")
public class Member {

    @Id
    @Column(name = "member_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int memberCode;

    @Column(name = "member_id")
    private String memberId;

    /* 설명. [Phase 2] BCrypt 해시가 저장된다. (예: $2a$10$... 60자)
     *  단방향 해시라 DB 를 열어봐도 원래 비밀번호를 알 수 없다. 비교는 PasswordEncoder.matches() 로 한다.
     * */
    @Column(name = "member_password")
    private String memberPassword;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "member_email")
    private String memberEmail;

    /* 설명. 회원 한 명은 권한을 하나만 가진다. ('ROLE_USER' 또는 'ROLE_ADMIN')
     *  'ROLE_' 접두사는 Spring Security 의 hasRole("ADMIN") 이 'ROLE_ADMIN' 으로 비교하는 규칙에 맞춘 것이다.
     * */
    @Column(name = "member_role")
    private String memberRole;

    public Member() {}

    public Member(String memberId, String memberPassword, String memberName, String memberEmail, String memberRole) {
        this.memberId = memberId;
        this.memberPassword = memberPassword;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.memberRole = memberRole;
    }

    public int getMemberCode() {
        return memberCode;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberPassword() {
        return memberPassword;
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

    @Override
    public String toString() {
        // 비밀번호는 로그에 남지 않도록 toString()에서 제외한다.
        return "Member{" +
                "memberCode=" + memberCode +
                ", memberId='" + memberId + '\'' +
                ", memberName='" + memberName + '\'' +
                ", memberEmail='" + memberEmail + '\'' +
                ", memberRole='" + memberRole + '\'' +
                '}';
    }
}
