import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/*
 * 로그인 상태. 헤더, 장바구니, 주문 내역, 관리자 화면이 모두 이 값을 본다.
 * 여러 화면이 같은 값을 봐야 하므로 Zustand 스토어에 둔다.
 *
 * [Phase 2] accessToken : 서버가 서명한 신원 증명서. api/client.js 의 인터셉터가 모든 요청에 붙인다.
 *           member      : 화면에 이름·권한을 보여주는 용도다. 권한 판단은 서버가 토큰으로 한다.
 *
 * persist 는 이 값들을 localStorage 에 저장한다. (F12 > Application > Local Storage > ohgi-auth)
 * 주의. localStorage 는 XSS 공격에 노출된다. (노션 '1. 인증과 인가의 기초' 3-4)
 */
export const useAuthStore = create(
    persist(
        (set) => ({
            accessToken: null,
            member: null,   // { memberCode, memberId, memberName, memberEmail, memberRole }

            login: (accessToken, member) => set({ accessToken, member }),

            // 로그아웃은 서버에 요청하지 않는다. 저장해 둔 토큰을 지우면 끝이다.
            // 헤더의 로그아웃 버튼과 api/client.js 의 401 처리가 모두 이 함수를 부른다.
            logout: () => {
                if (import.meta.env.DEV) {
                    console.log('[로그아웃] 저장해 둔 토큰을 지운다. 서버에는 요청하지 않는다.');
                }
                set({ accessToken: null, member: null });
            },
        }),
        { name: 'ohgi-auth' },
    ),
);

export function isAdmin(member) {
    return member?.memberRole === 'ROLE_ADMIN';
}
