package com.ohgiraffers.restaurant.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    /* 로그인할 때 아이디로 회원을 찾는 메소드 */
    Optional<Member> findByMemberId(String memberId);

    /* 회원가입할 때 아이디 중복 여부를 확인하는 메소드 */
    boolean existsByMemberId(String memberId);
}
