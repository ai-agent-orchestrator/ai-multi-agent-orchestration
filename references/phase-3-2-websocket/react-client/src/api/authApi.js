import { api, resultOf } from './client.js';

/*
 * 각 함수 위의 주석은 이 요청을 받는 Spring 컨트롤러 메서드다.
 * React 의 요청 한 줄과 서버의 @XxxMapping 한 줄이 짝을 이룬다.
 */

// AuthController.signup()   POST /api/auth/signup   (@RequestBody SignupRequest)
export async function signup({ memberId, memberPassword, memberName, memberEmail }) {
    const response = await api.post('/api/auth/signup', { memberId, memberPassword, memberName, memberEmail });
    return resultOf(response).member;
}

// AuthController.login()    POST /api/auth/login    (@RequestBody LoginRequest)
// [Phase 2] 응답에 서버가 서명한 accessToken 이 함께 온다.
export async function login({ memberId, memberPassword }) {
    const response = await api.post('/api/auth/login', { memberId, memberPassword });
    const { accessToken, member } = resultOf(response);
    return { accessToken, member };
}

// MemberController.findMyInfo()   GET /api/members/me   (@AuthenticationPrincipal)
// [Phase 2] 경로에 memberCode 가 없다. 서버가 요청 헤더의 토큰에서 회원을 꺼낸다.
export async function fetchMyInfo() {
    const response = await api.get('/api/members/me');
    return resultOf(response).member;
}
