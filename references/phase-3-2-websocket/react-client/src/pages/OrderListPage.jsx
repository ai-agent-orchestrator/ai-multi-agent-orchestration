import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';

import { fetchMyOrders } from '../api/orderApi.js';
import Button from '../components/Button.jsx';
import OrderCard from '../components/OrderCard.jsx';
import StatusBlock from '../components/StatusBlock.jsx';
import styles from './OrderListPage.module.css';

export default function OrderListPage() {
    const location = useLocation();
    const navigate = useNavigate();

    // 장바구니에서 주문을 마치고 넘어오면 주문 번호가 state 로 함께 온다.
    const orderedCode = location.state?.orderedCode;

    const [orders, setOrders] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                // [Phase 2] 주소에 memberCode 가 없다. 서버가 토큰의 주인을 보고 그 사람의 주문만 준다.
                setOrders(await fetchMyOrders(controller.signal));
            } catch (err) {
                if (controller.signal.aborted) return;
                setError(err.message);
            }
        }

        load();
        return () => controller.abort();
    }, []);

    return (
        <div className={styles.page}>
            <h1 className="title2">주문 내역</h1>

            {orderedCode && (
                <p className={`${styles.done} label1`}>주문 #{orderedCode} 이(가) 완료되었습니다.</p>
            )}

            {error ? (
                <StatusBlock tone="error" title="주문 내역을 불러오지 못했습니다" description={error} />
            ) : orders === null ? (
                <p className={`${styles.loading} body1`}>불러오는 중…</p>
            ) : orders.length === 0 ? (
                <StatusBlock
                    title="아직 주문한 내역이 없습니다"
                    action={<Button onClick={() => navigate('/menus')}>메뉴 보러 가기</Button>}
                />
            ) : (
                <ul className={styles.list}>
                    {orders.map((order) => (
                        <li key={order.orderCode}>
                            <OrderCard order={order} />
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
