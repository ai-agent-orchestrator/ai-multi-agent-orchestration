import { Navigate, useLocation } from 'react-router';

import StatusBlock from './StatusBlock.jsx';
import { isAdmin, useAuthStore } from '../store/authStore.js';

/**
 * 관리자가 아니면 관리자 화면을 보여주지 않는다.
 *
 * 이 검사는 브라우저에 저장된 memberRole 만 본다. 화면을 보여줄지 말지를 정하는 편의 기능이다.
 * [Phase 2] F12 에서 memberRole 을 고치면 관리자 화면이 '열리기는' 하지만, 화면이 부르는 API 는
 *   서버가 토큰의 권한으로 다시 검사하므로 전부 403 으로 거절된다. 진짜 권한 검사는 서버에 있다.
 */
export default function RequireAdmin({ children }) {
    const member = useAuthStore((state) => state.member);
    const location = useLocation();

    if (!member) {
        return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
    }
    if (!isAdmin(member)) {
        return (
            <StatusBlock
                tone="error"
                title="관리자만 볼 수 있는 화면입니다"
                description="관리자 계정으로 다시 로그인해 주세요."
            />
        );
    }
    return children;
}
