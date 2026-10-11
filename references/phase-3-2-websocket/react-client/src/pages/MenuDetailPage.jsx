import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';

import { addCartItem } from '../api/cartApi.js';
import { fetchMenu } from '../api/menuApi.js';
import AllergenChips from '../components/AllergenChips.jsx';
import Button from '../components/Button.jsx';
import MenuImage from '../components/MenuImage.jsx';
import QuantityStepper from '../components/QuantityStepper.jsx';
import StatusBlock from '../components/StatusBlock.jsx';
import { useAuthStore } from '../store/authStore.js';
import { useCartStore } from '../store/cartStore.js';
import { formatPrice } from '../format.js';
import styles from './MenuDetailPage.module.css';

export default function MenuDetailPage() {
    // 주소의 :menuCode 자리 값. 문자열로 온다. (/menus/12 -> '12')
    const { menuCode } = useParams();

    const [menu, setMenu] = useState(null);
    const [error, setError] = useState(null);
    const [amount, setAmount] = useState(1);
    const [added, setAdded] = useState(false);
    const [addError, setAddError] = useState(null);

    const member = useAuthStore((state) => state.member);
    const setCountFrom = useCartStore((state) => state.setCountFrom);
    const navigate = useNavigate();
    const location = useLocation();

    // 주소의 menuCode 가 바뀌면 다시 받는다. 의존성 배열에 menuCode 를 넣는 이유다.
    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            setMenu(null);
            setError(null);
            setAmount(1);
            setAdded(false);
            setAddError(null);
            try {
                setMenu(await fetchMenu(menuCode, controller.signal));
            } catch (err) {
                if (controller.signal.aborted) return;
                setError(err);
            }
        }

        load();
        return () => controller.abort();
    }, [menuCode]);

    async function handleAdd() {
        // 장바구니는 회원별로 서버에 저장되므로 로그인이 필요하다. 로그인 후 이 화면으로 돌아오게 한다.
        if (!member) {
            navigate('/login', { state: { from: location.pathname } });
            return;
        }

        setAdded(false);
        setAddError(null);
        try {
            const cartItems = await addCartItem({ menuCode: menu.menuCode, cartAmount: amount });
            setCountFrom(cartItems);   // 서버가 돌려준 장바구니로 헤더의 개수를 다시 센다
            setAdded(true);
        } catch (err) {
            // 예) 한 메뉴를 99개보다 많이 담으려고 한 경우 -> 서버가 400 과 이유를 돌려준다.
            setAddError(err.message);
        }
    }

    if (error) {
        return (
            <StatusBlock
                tone="error"
                title={error.status === 404 ? '메뉴를 찾을 수 없습니다' : '메뉴를 불러오지 못했습니다'}
                description={error.message}
                action={<Button onClick={() => navigate('/menus')}>메뉴 목록으로</Button>}
            />
        );
    }

    if (!menu) {
        return <p className={`${styles.loading} body1`}>불러오는 중…</p>;
    }

    const orderable = menu.orderableStatus === 'Y';

    return (
        <article className={styles.page}>
            <Link to="/menus" className={`${styles.back} label1`}>
                &larr; 메뉴 목록
            </Link>

            <div className={styles.layout}>
                <div className={styles.art}>
                    <MenuImage path={menu.menuImageUrl} alt={menu.menuName} />
                </div>

                <div className={styles.info}>
                    <span className={`${styles.category} label1`}>{menu.categoryName}</span>
                    <h1 className="title2">{menu.menuName}</h1>
                    <p className={`${styles.price} title3`}>{formatPrice(menu.menuPrice)}</p>
                    <p className={`${styles.desc} body1-reading`}>{menu.menuDescription}</p>

                    <section className={styles.section}>
                        <h2 className="headline2">재료</h2>
                        <p className="body2">{menu.menuIngredients ?? '정보 없음'}</p>
                    </section>

                    <section className={styles.section}>
                        <h2 className="headline2">알레르기 유발 성분</h2>
                        <AllergenChips allergens={menu.allergens} />
                    </section>

                    <div className={styles.order}>
                        {orderable ? (
                            <>
                                <QuantityStepper value={amount} onChange={setAmount} />
                                <Button size="large" onClick={handleAdd}>
                                    {member
                                        ? `${formatPrice(menu.menuPrice * amount)} 장바구니에 담기`
                                        : '로그인하고 장바구니에 담기'}
                                </Button>
                            </>
                        ) : (
                            <Button size="large" disabled>
                                지금은 주문할 수 없는 메뉴입니다
                            </Button>
                        )}
                    </div>

                    {added && (
                        <p className={`${styles.added} label1`}>
                            장바구니에 담았습니다. <Link to="/cart">장바구니 보기</Link>
                        </p>
                    )}
                    {addError && <p className={`${styles.addError} label1`}>{addError}</p>}
                </div>
            </div>
        </article>
    );
}
