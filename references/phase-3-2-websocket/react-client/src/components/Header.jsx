import { useEffect } from 'react';
import { Link, NavLink } from 'react-router';

import { fetchCart } from '../api/cartApi.js';
import { isAdmin, useAuthStore } from '../store/authStore.js';
import { useCartStore } from '../store/cartStore.js';
import styles from './Header.module.css';

export default function Header() {
    const member = useAuthStore((state) => state.member);
    const logout = useAuthStore((state) => state.logout);
    const cartCount = useCartStore((state) => state.count);
    const setCountFrom = useCartStore((state) => state.setCountFrom);
    const resetCount = useCartStore((state) => state.reset);

    // 로그인 상태가 바뀔 때마다(새로고침, 로그인, 로그아웃) 장바구니 개수를 서버에서 다시 받는다.
    useEffect(() => {
        if (!member) {
            resetCount();
            return;
        }
        const controller = new AbortController();
        fetchCart(controller.signal)
            .then(setCountFrom)
            .catch(() => {});   // 배지는 부가 정보라 실패해도 화면에 오류를 띄우지 않는다
        return () => controller.abort();
    }, [member, setCountFrom, resetCount]);

    /*
     * 로그아웃은 스토어의 회원 정보만 지운다. 화면 이동은 하지 않는다.
     * - 메뉴 목록처럼 누구나 보는 화면이면 그 자리에 그대로 남는다.
     * - 주문 내역·관리자 화면처럼 로그인이 필요한 화면이면
     *   RequireAuth / RequireAdmin 이 회원 정보가 사라진 것을 보고 로그인 화면으로 보낸다.
     * 여기서 navigate 까지 부르면 위의 이동과 서로 겹쳐서, 어디로 갈지가 그때그때 달라진다.
     */
    function handleLogout() {
        logout();
    }

    // NavLink 는 현재 주소와 같으면 isActive 가 true 가 된다.
    const navClass = ({ isActive }) => `${styles.link} label1 ${isActive ? styles.active : ''}`;

    return (
        <header className={styles.header}>
            <div className={styles.inner}>
                <Link to="/menus" className={`${styles.brand} headline1`}>
                    오지라퍼 레스토랑
                </Link>

                <nav className={styles.nav}>
                    <NavLink to="/menus" className={navClass}>메뉴</NavLink>
                    <NavLink to="/cart" className={navClass}>
                        장바구니
                        {member && cartCount > 0 && <span className={`${styles.badge} caption1`}>{cartCount}</span>}
                    </NavLink>
                    {member && <NavLink to="/orders" className={navClass}>주문 내역</NavLink>}

                    {/* 관리자 메뉴를 숨기는 것은 화면의 편의다. 실제로 막는 것은 서버다. (관리자가 아닌 토큰은 403) */}
                    {isAdmin(member) && (
                        <>
                            <span className={styles.divider} aria-hidden="true" />
                            <NavLink to="/admin/menus" className={navClass}>메뉴 관리</NavLink>
                            <NavLink to="/admin/orders" className={navClass}>주문 관리</NavLink>
                        </>
                    )}
                </nav>

                <div className={styles.auth}>
                    {member ? (
                        <>
                            <span className={`${styles.who} label1`}>{member.memberName}님</span>
                            <button type="button" className={`${styles.textButton} label1`} onClick={handleLogout}>
                                로그아웃
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className={`${styles.textButton} label1`}>로그인</Link>
                            <Link to="/signup" className={`${styles.textButton} label1`}>회원가입</Link>
                        </>
                    )}
                </div>
            </div>
        </header>
    );
}
