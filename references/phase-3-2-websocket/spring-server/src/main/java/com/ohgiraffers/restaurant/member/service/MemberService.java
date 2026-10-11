package com.ohgiraffers.restaurant.member.service;

import com.ohgiraffers.restaurant.common.exception.DuplicateException;
import com.ohgiraffers.restaurant.common.exception.LoginFailedException;
import com.ohgiraffers.restaurant.common.exception.NotFoundException;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import com.ohgiraffers.restaurant.common.security.JwtTokenProvider;
import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.member.domain.MemberRepository;
import com.ohgiraffers.restaurant.member.dto.LoginRequest;
import com.ohgiraffers.restaurant.member.dto.LoginResponse;
import com.ohgiraffers.restaurant.member.dto.MemberResponse;
import com.ohgiraffers.restaurant.member.dto.SignupRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberService(MemberRepository memberRepository,
                         PasswordEncoder passwordEncoder,
                         AuthenticationManager authenticationManager,
                         JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /* 목차. 1. 회원가입 */
    /**
     * 새 회원을 등록한다. 비밀번호는 BCrypt 로 암호화해서 저장한다.
     *
     * @param signupRequest 회원가입 정보
     * @return 가입된 회원 정보
     * @throws DuplicateException 이미 사용 중인 아이디인 경우
     */
    @Transactional
    public MemberResponse signup(SignupRequest signupRequest) {

        if (memberRepository.existsByMemberId(signupRequest.getMemberId())) {
            throw new DuplicateException("이미 사용 중인 아이디입니다: " + signupRequest.getMemberId());
        }

        /* 설명. [Phase 2] 평문 대신 BCrypt 해시를 저장한다.
         *  예) user1234 -> $2a$10$Qm3... (60자, 같은 비밀번호라도 가입할 때마다 다른 값이 된다)
         * */
        Member member = new Member(
                signupRequest.getMemberId(),
                passwordEncoder.encode(signupRequest.getMemberPassword()),
                signupRequest.getMemberName(),
                signupRequest.getMemberEmail(),
                "ROLE_USER"
        );

        return MemberResponse.from(memberRepository.save(member));
    }

    /* 목차. 2. 로그인 */
    /**
     * 아이디와 비밀번호를 확인하고, 일치하면 Access Token 을 발급한다.
     *
     * <p>비밀번호 비교는 직접 하지 않고 Spring Security 의 AuthenticationManager 에게 맡긴다.
     * AuthenticationManager 는 CustomUserDetailsService 로 회원을 찾고, PasswordEncoder 로 비밀번호를 비교한다.</p>
     *
     * <p>Phase 1 과 달라진 점: 로그인이 끝나면 서버가 서명한 토큰을 돌려준다.
     * 이후 요청에서 클라이언트는 memberCode 대신 이 토큰을 보내고, 서버는 토큰에서 회원을 꺼낸다.</p>
     *
     * @param loginRequest 로그인 정보
     * @return Access Token 과 회원 정보
     * @throws LoginFailedException 아이디가 없거나 비밀번호가 틀린 경우
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {

        System.out.println("[MemberService] 로그인 시작 -> AuthenticationManager 에게 아이디 · 비밀번호 검사를 맡긴다");

        Authentication authentication;
        try {
            // 아직 인증되지 않은 '아이디 + 비밀번호' 묶음을 만든다.
            UsernamePasswordAuthenticationToken loginToken = UsernamePasswordAuthenticationToken.unauthenticated(
                    loginRequest.getMemberId(), loginRequest.getMemberPassword());

            // 검사를 맡긴다. 안에서 CustomUserDetailsService 로 회원을 찾고 PasswordEncoder 로 비밀번호를 비교한다.
            // 통과하면 '인증된' Authentication 이 돌아오고, 실패하면 AuthenticationException 이 난다.
            authentication = authenticationManager.authenticate(loginToken);

        } catch (AuthenticationException e) {
            System.out.println("[MemberService] 인증 실패 : " + e.getClass().getSimpleName());

            /* 설명. 아이디가 없어도, 비밀번호가 틀려도 Spring Security 는 똑같이 BadCredentialsException 을 던진다.
             *  어떤 아이디가 가입되어 있는지 밖에서 알아낼 수 없게 하기 위해서다. 응답 메시지도 하나로 맞춘다.
             * */
            throw new LoginFailedException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 인증에 성공하면 principal 에 CustomUserDetailsService 가 만든 CustomUserDetails 가 들어 있다.
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        System.out.println("[MemberService] 인증 성공 : " + user);

        // 이 회원의 Access Token 을 발급한다.
        String accessToken = jwtTokenProvider.createAccessToken(user);

        System.out.println("[MemberService] 로그인 완료 -> 토큰과 회원 정보를 응답에 담는다");

        // 토큰과 함께 화면에 보여줄 회원 정보를 돌려준다.
        return new LoginResponse(accessToken, findMember(user.getMemberCode()));
    }

    /* 목차. 3. 회원 정보 조회 */
    @Transactional(readOnly = true)
    public MemberResponse findMember(int memberCode) {

        Member member = memberRepository.findById(memberCode)
                .orElseThrow(() -> new NotFoundException("해당 회원이 존재하지 않습니다. memberCode=" + memberCode));

        return MemberResponse.from(member);
    }
}
