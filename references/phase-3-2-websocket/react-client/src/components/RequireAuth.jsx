import { Navigate, useLocation } from 'react-router';

import { useAuthStore } from '../store/authStore.js';

/**
 * 로그인하지 않았으면 로그인 화면으로 보낸다.
 * 로그인이 끝나면 원래 가려던 주소로 돌아올 수 있도록 state.from 에 담아 둔다.
 *
 * 이것은 화면 이동을 막는 것이다. API 는 서버가 토큰으로 따로 막는다. (토큰 없이 요청하면 401)
 */
export default function RequireAuth({ children }) {
    const member = useAuthStore((state) => state.member);
    const location = useLocation();

    if (!member) {
        return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
    }
    return children;
}
