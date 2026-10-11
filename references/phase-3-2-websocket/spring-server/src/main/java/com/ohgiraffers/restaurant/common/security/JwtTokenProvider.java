package com.ohgiraffers.restaurant.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/* 설명. Access Token 을 발급하고 검증하는 부품
 *  base64-key-generator 의 JjwtGenerator 에서 해 본 발급(builder)과 검증(parser)을 그대로 옮겨왔다.
 *  ----------------------------------------------------------------------------------
 *  토큰에 담는 값 (Payload)
 *  - sub      : 회원코드. 서버가 "누구의 요청인지" 알아내는 값이다
 *  - memberId : 로그인 아이디
 *  - role     : 권한 ('ROLE_USER' 또는 'ROLE_ADMIN')
 *  - iat, exp : 발급 시각, 만료 시각
 *  주의. 페이로드는 암호화되지 않는다. 누구나 Base64URL 디코딩으로 읽을 수 있으므로 비밀번호 같은 값은 넣지 않는다.
 * */
@Component
public class JwtTokenProvider {

    // 서명과 검증에 쓰는 비밀키 (서버만 안다)
    private final SecretKey secretKey;
    // Access Token 유효시간 (밀리초)
    private final long accessTokenExpiration;

    /* 설명. application.yaml 의 jwt.secret(Base64URL 문자열)을 디코딩해서 서명용 비밀키로 만든다.
     *  SecretKeyGenerator 가 Encoders.BASE64URL 로 만들었으므로 읽을 때도 Decoders.BASE64URL 을 쓴다.
     *  방식이 다르면 서버가 뜰 때 DecodingException 이 난다. (예: Decoders.BASE64 는 '_' 를 모른다)
     *  Keys.hmacShaKeyFor() 는 키가 256비트보다 짧으면 WeakKeyException 을 던져 서버 실행 자체를 막는다.
     * */
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        // Base64URL 문자열 -> 바이트 배열 -> HMAC-SHA 비밀키 객체
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /* 목차. 1. Access Token 발급 (로그인에 성공했을 때 MemberService 가 부른다) */
    public String createAccessToken(CustomUserDetails user) {

        System.out.println("[JwtTokenProvider] Access Token 발급 : memberCode=" + user.getMemberCode()
                + ", memberId=" + user.getMemberId() + ", role=" + user.getMemberRole());

        // 발급 시각. 만료 시각도 이 시각을 기준으로 계산한다.
        Date now = new Date();

        return Jwts.builder()
                // 헤더에 타입(typ: "JWT")을 적는다. 알고리즘(alg: "HS256")은 signWith() 가 키를 보고 채운다.
                .header().type("JWT").and()
                // 토큰의 주인(sub). 회원코드를 문자열로 넣는다. (표준에서 sub 는 문자열이다)
                .subject(String.valueOf(user.getMemberCode()))
                // 우리가 정한 값(커스텀 클레임). 필터가 DB 를 조회하지 않고도 사용자를 만들 수 있게 필요한 값만 담는다.
                .claim("memberId", user.getMemberId())
                .claim("role", user.getMemberRole())
                // 발급 시각(iat)과 만료 시각(exp)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                // 비밀키로 서명한다.
                .signWith(secretKey)
                // 'Header.Payload.Signature' 세 부분을 점(.)으로 이은 문자열로 만든다.
                .compact();
    }

    /* 목차. 2. Access Token 검증 (요청마다 JwtAuthenticationFilter 가 부른다) */
    /**
     * 토큰의 서명과 만료 시각을 검증하고, 통과하면 토큰 안의 값으로 인증된 사용자 객체를 만든다.
     *
     * <p>DB 를 조회하지 않는다. 서명이 맞다는 것은 "이 서버가 발급한 뒤로 한 글자도 바뀌지 않았다"는 뜻이므로
     * 토큰 안의 회원코드와 권한을 그대로 믿을 수 있다. 토큰 방식이 무상태(stateless)라는 말이 이것이다.</p>
     *
     * @param token Authorization 헤더에서 꺼낸 토큰 문자열
     * @return 토큰 주인의 정보 (비밀번호는 없다)
     * @throws JwtException 서명이 맞지 않거나(위조), 만료되었거나, 형식이 잘못된 경우
     */
    public CustomUserDetails parseAccessToken(String token) {

        Claims claims = Jwts.parser()
                // 이 비밀키로 서명을 다시 계산해서 토큰의 서명과 비교한다.
                .verifyWith(secretKey)
                .build()
                // 형식 -> 서명 -> 만료 순서로 확인하고, 하나라도 실패하면 예외를 던진다.
                .parseSignedClaims(token)
                // 검증을 통과한 페이로드(클레임)를 꺼낸다.
                .getPayload();

        System.out.println("[JwtTokenProvider] Access Token 검증 통과 : sub=" + claims.getSubject()
                + ", 만료 시각=" + claims.getExpiration());

        // 토큰 안의 값으로 사용자 객체를 만든다. 비밀번호는 토큰에 없으므로 null 이다.
        return new CustomUserDetails(
                Integer.parseInt(claims.getSubject()),
                claims.get("memberId", String.class),
                null,
                claims.get("role", String.class)
        );
    }

    /* 목차. 3. 유효시간 조회 */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
