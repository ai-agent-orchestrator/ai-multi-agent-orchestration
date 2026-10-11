import MenuImage from './MenuImage.jsx';
import { formatDateTime, formatPrice, PAYMENT_TYPE_LABEL } from '../format.js';
import styles from './OrderCard.module.css';

/**
 * 주문 한 건. 내 주문 내역과 관리자 주문 관리 화면이 함께 쓴다.
 * showMember 를 켜면 주문한 회원 이름을 함께 보여준다. (관리자 화면용)
 */
export default function OrderCard({ order, showMember = false }) {
    return (
        <article className={styles.card}>
            <header className={styles.head}>
                <div className={styles.meta}>
                    <span className="headline2">주문 #{order.orderCode}</span>
                    <span className={`${styles.sub} label2`}>
                        {formatDateTime(order.orderDatetime)}
                        {showMember && ` · ${order.memberName}`}
                    </span>
                </div>
                <span className={`${styles.payment} label2`}>
                    {PAYMENT_TYPE_LABEL[order.paymentType] ?? order.paymentType} 결제
                </span>
            </header>

            <ul className={styles.items}>
                {order.orderMenus.map((item) => (
                    <li key={item.menuCode} className={styles.item}>
                        <div className={styles.thumb}>
                            <MenuImage path={item.menuImageUrl} compact />
                        </div>
                        <span className="body2">{item.menuName}</span>
                        <span className={`${styles.amount} label2`}>{item.orderAmount}개</span>
                    </li>
                ))}
            </ul>

            <footer className={styles.total}>
                <span className="label1">결제 금액</span>
                <span className="headline1">{formatPrice(order.totalOrderPrice)}</span>
            </footer>
        </article>
    );
}
