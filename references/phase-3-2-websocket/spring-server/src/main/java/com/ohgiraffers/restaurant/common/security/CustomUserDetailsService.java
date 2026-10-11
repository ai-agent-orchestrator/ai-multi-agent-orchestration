package com.ohgiraffers.restaurant.common.security;

import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.member.domain.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/* 설명. 로그인할 때 Spring Security 가 "이 아이디의 회원을 찾아달라"고 부르는 창구
 *  ----------------------------------------------------------------------------------
 *  로그인의 흐름 (MemberService.login 참고)
 *  1) MemberService 가 AuthenticationManager 에게 아이디와 비밀번호를 넘긴다.
 *  2) AuthenticationManager 가 이 클래스의 loadUserByUsername(아이디) 를 부른다.   <- 여기
 *  3) 돌려받은 CustomUserDetails 의 비밀번호(BCrypt)와 입력한 비밀번호를 PasswordEncoder 로 비교한다.
 *  4) 일치하면 인증 성공, 아니면 BadCredentialsException 을 던진다.
 *  ----------------------------------------------------------------------------------
 *  즉 이 클래스는 '회원을 찾아오는 일' 하나만 한다. 비밀번호 비교는 Spring Security 가 한다.
 *  UserDetailsService 를 구현한 빈이 하나 있으면 Spring Security 가 자동으로 찾아서 쓴다.
 *  ----------------------------------------------------------------------------------
 *  주의. 이 클래스는 로그인할 때만 쓰인다. 로그인 이후의 요청은 토큰만 보고 인증하므로 DB 를 다시 조회하지 않는다.
 * */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // 회원 조회용 리포지토리 (member 도메인의 것을 그대로 쓴다)
    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /* 설명. 매개변수 이름은 memberId 지만, 인터페이스에서는 username 이라고 부른다. (로그인 아이디라는 뜻) */
    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {

        System.out.println("[CustomUserDetailsService] AuthenticationManager 가 회원 조회를 요청함 : memberId=" + memberId);

        // 아이디로 회원을 찾는다. 없으면 UsernameNotFoundException 을 던진다.
        // (이 예외는 Spring Security 가 BadCredentialsException 으로 바꿔서, 밖에서는 비밀번호가 틀린 경우와 구별되지 않는다)
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> {
                    System.out.println("[CustomUserDetailsService] 없는 아이디 : " + memberId);
                    return new UsernameNotFoundException("존재하지 않는 아이디입니다: " + memberId);
                });

        System.out.println("[CustomUserDetailsService] 회원 찾음 -> 비밀번호 비교는 PasswordEncoder 가 한다 : memberCode="
                + member.getMemberCode() + ", role=" + member.getMemberRole());

        // 찾은 회원을 Spring Security 가 이해하는 모양(UserDetails)으로 감싸서 돌려준다. (BCrypt 비밀번호 포함)
        return new CustomUserDetails(
                member.getMemberCode(),
                member.getMemberId(),
                member.getMemberPassword(),
                member.getMemberRole()
        );
    }
}
