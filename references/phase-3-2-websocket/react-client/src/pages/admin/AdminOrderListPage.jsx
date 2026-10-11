import { useEffect, useState } from 'react';

import { fetchAllOrders } from '../../api/adminApi.js';
import OrderCard from '../../components/OrderCard.jsx';
import StatusBlock from '../../components/StatusBlock.jsx';
import styles from '../OrderListPage.module.css';

export default function AdminOrderListPage() {
    const [orders, setOrders] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                setOrders(await fetchAllOrders(controller.signal));
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
            <h1 className="title2">주문 관리</h1>

            {error ? (
                <StatusBlock tone="error" title="주문 내역을 불러오지 못했습니다" description={error} />
            ) : orders === null ? (
                <p className={`${styles.loading} body1`}>불러오는 중…</p>
            ) : orders.length === 0 ? (
                <StatusBlock title="아직 들어온 주문이 없습니다" />
            ) : (
                <ul className={styles.list}>
                    {orders.map((order) => (
                        <li key={order.orderCode}>
                            <OrderCard order={order} showMember />
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
