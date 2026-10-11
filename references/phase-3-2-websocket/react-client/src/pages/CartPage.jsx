import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';

import { changeCartItem, fetchCart, removeCartItem } from '../api/cartApi.js';
import { placeOrder } from '../api/orderApi.js';
import Button from '../components/Button.jsx';
import MenuImage from '../components/MenuImage.jsx';
import QuantityStepper from '../components/QuantityStepper.jsx';
import StatusBlock from '../components/StatusBlock.jsx';
import { useCartStore } from '../store/cartStore.js';
import { formatPrice, PAYMENT_TYPE_LABEL } from '../format.js';
import fieldStyles from '../components/Field.module.css';
import styles from './CartPage.module.css';

/*
 * 장바구니는 서버(DB)에 있다. 이 화면은 들어올 때 받아 오고, 수량을 바꾸거나 뺄 때마다 서버에 반영한 뒤
 * 서버가 돌려준 장바구니로 다시 그린다. (로그인한 회원만 들어올 수 있다. App.jsx 의 RequireAuth)
 */
export default function CartPage() {
    const setCountFrom = useCartStore((state) => state.setCountFrom);
    const resetCount = useCartStore((state) => state.reset);
    const navigate = useNavigate();

    const [items, setItems] = useState(null);   // null 이면 아직 받는 중
    const [loadError, setLoadError] = useState(null);
    const [paymentType, setPaymentType] = useState('CARD');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                const cartItems = await fetchCart(controller.signal);
                setItems(cartItems);
                setCountFrom(cartItems);
            } catch (err) {
                if (controller.signal.aborted) return;
                setLoadError(err.message);
            }
        }

        load();
        return () => controller.abort();
    }, [setCountFrom]);

    // 수량 변경 · 빼기 : 서버에 반영하고, 서버가 돌려준 장바구니로 화면과 헤더 개수를 다시 그린다.
    async function apply(request) {
        setError(null);
        try {
            const cartItems = await request;
            setItems(cartItems);
            setCountFrom(cartItems);
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleOrder() {
        setSubmitting(true);
        setError(null);
        try {
            // [Phase 2] memberCode 를 보내지 않는다. 서버가 토큰에서 주문한 사람을 꺼낸다.
            const order = await placeOrder({ paymentType, items });
            resetCount();   // 주문이 끝나면 서버가 장바구니를 비운다
            navigate('/orders', { state: { orderedCode: order.orderCode } });
        } catch (err) {
            // 예) 담아 둔 사이에 관리자가 메뉴를 품절 처리한 경우 -> 서버가 400 과 이유를 돌려준다.
            setError(err.message);
            setSubmitting(false);
        }
    }

    if (loadError) {
        return <StatusBlock tone="error" title="장바구니를 불러오지 못했습니다" description={loadError} />;
    }

    if (items === null) {
        return <p className="body1">불러오는 중…</p>;
    }

    if (items.length === 0) {
        return (
            <StatusBlock
                title="장바구니가 비어 있습니다"
                description="메뉴를 골라 담아 보세요."
                action={<Button onClick={() => navigate('/menus')}>메뉴 보러 가기</Button>}
            />
        );
    }

    // 화면에 보여주는 합계일 뿐이다. 실제 결제 금액은 서버가 DB 가격으로 다시 계산한다.
    const total = items.reduce((sum, item) => sum + item.menuPrice * item.cartAmount, 0);

    return (
        <div className={styles.page}>
            <h1 className="title2">장바구니</h1>

            <ul className={styles.items}>
                {items.map((item) => (
                    <li key={item.menuCode} className={styles.item}>
                        <div className={styles.thumb}>
                            <MenuImage path={item.menuImageUrl} compact />
                        </div>
                        <div className={styles.name}>
                            <Link to={`/menus/${item.menuCode}`} className="headline2">{item.menuName}</Link>
                            <span className={`${styles.unit} label2`}>
                                {formatPrice(item.menuPrice)}
                                {item.orderableStatus === 'N' && ' · 지금은 주문할 수 없는 메뉴'}
                            </span>
                        </div>
                        <QuantityStepper
                            value={item.cartAmount}
                            onChange={(n) => apply(changeCartItem(item.menuCode, n))}
                        />
                        <span className={`${styles.subtotal} headline2`}>
                            {formatPrice(item.menuPrice * item.cartAmount)}
                        </span>
                        <button
                            type="button"
                            className={`${styles.remove} label2`}
                            onClick={() => apply(removeCartItem(item.menuCode))}
                        >
                            삭제
                        </button>
                    </li>
                ))}
            </ul>

            <section className={styles.summary}>
                <fieldset className={styles.payment}>
                    <legend className="headline2">결제 수단</legend>
                    {Object.entries(PAYMENT_TYPE_LABEL).map(([value, label]) => (
                        <label key={value} className={`${styles.radio} label1`}>
                            <input
                                type="radio"
                                name="paymentType"
                                value={value}
                                checked={paymentType === value}
                                onChange={(e) => setPaymentType(e.target.value)}
                            />
                            {label}
                        </label>
                    ))}
                </fieldset>

                <div className={styles.total}>
                    <span className="label1">총 금액</span>
                    <span className="title3">{formatPrice(total)}</span>
                </div>

                {error && <p className={`${fieldStyles.error} body2`}>{error}</p>}

                <Button size="large" onClick={handleOrder} disabled={submitting}>
                    {submitting ? '주문하는 중…' : '주문하기'}
                </Button>
            </section>
        </div>
    );
}
