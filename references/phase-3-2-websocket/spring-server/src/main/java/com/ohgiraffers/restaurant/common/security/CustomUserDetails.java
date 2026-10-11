package com.ohgiraffers.restaurant.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/* 설명. Spring Security 가 이해하는 '인증된 사용자' 객체
 *  ----------------------------------------------------------------------------------
 *  Spring Security 는 우리 프로젝트의 Member 엔티티를 모른다.
 *  대신 UserDetails 라는 약속(인터페이스)을 정해두고, 그 모양으로 사용자 정보를 달라고 한다.
 *  그래서 Member 를 직접 넘기지 않고, 필요한 값만 담은 이 클래스로 감싸서 넘긴다.
 *  (Member 에 직접 구현하지 않는 이유 : 노션 '4. JWT 인증·인가 구현' 2절)
 *  ----------------------------------------------------------------------------------
 *  이 객체는 두 곳에서 만들어진다.
 *  1) 로그인할 때 : CustomUserDetailsService 가 DB 에서 찾은 회원으로 만든다. (비밀번호 포함)
 *  2) 요청마다    : JwtAuthenticationFilter 가 토큰 안의 값으로 만든다. (비밀번호 없음, DB 조회 없음)
 *  컨트롤러에서는 @AuthenticationPrincipal CustomUserDetails 로 꺼내 쓴다.
 * */
public class CustomUserDetails implements UserDetails {

    // 회원코드 (토큰의 sub). 컨트롤러가 "누구의 요청인지" 알아낼 때 쓴다.
    private final int memberCode;
    // 로그인 아이디 (Spring Security 가 말하는 username)
    private final String memberId;
    // BCrypt 로 암호화된 비밀번호. 로그인할 때만 들어 있고, 토큰으로 만든 객체에서는 null 이다.
    private final String password;
    // 권한 ('ROLE_USER' 또는 'ROLE_ADMIN')
    private final String memberRole;

    public CustomUserDetails(int memberCode, String memberId, String password, String memberRole) {
        this.memberCode = memberCode;
        this.memberId = memberId;
        this.password = password;
        this.memberRole = memberRole;
    }

    /* 설명. 이 사용자가 가진 권한 목록
     *  회원 한 명은 권한을 하나만 가지므로('ROLE_USER' 또는 'ROLE_ADMIN') 목록에 하나만 담는다.
     *  SecurityConfig 의 hasRole("ADMIN") 은 이 목록에 'ROLE_ADMIN' 이 있는지를 확인한다.
     *  (hasRole 은 앞에 'ROLE_' 을 붙여서 비교하므로 DB 에 'ROLE_' 접두사를 붙여 저장해 두었다)
     * */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(memberRole));
    }

    // 로그인할 때 AuthenticationManager 가 이 값과 입력한 비밀번호를 PasswordEncoder 로 비교한다.
    @Override
    public String getPassword() {
        return password;
    }

    // Spring Security 가 말하는 'username' 은 로그인 아이디다. (우리 프로젝트의 memberId)
    @Override
    public String getUsername() {
        return memberId;
    }

    /* 참고. UserDetails 에는 계정 만료 · 잠김 · 비밀번호 만료 · 사용 가능 여부를 묻는 메서드도 있다.
     *  (isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired, isEnabled)
     *  모두 기본값이 true 인 default 메서드라, 이 프로젝트처럼 그런 기능이 없으면 구현하지 않아도 된다.
     * */

    public int getMemberCode() {
        return memberCode;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    @Override
    public String toString() {
        // 비밀번호는 로그에 남지 않도록 제외한다.
        return "CustomUserDetails{memberCode=" + memberCode + ", memberId='" + memberId + "', memberRole='" + memberRole + "'}";
    }
}
