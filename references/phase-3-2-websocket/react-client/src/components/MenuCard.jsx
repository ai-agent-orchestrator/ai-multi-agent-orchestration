import { Link } from 'react-router';

import MenuImage from './MenuImage.jsx';
import { formatPrice } from '../format.js';
import styles from './MenuCard.module.css';

/**
 * 메뉴 목록의 카드 한 장. 목록 응답(result.menuPage.content)의 한 항목을 그대로 받는다.
 * 목록 응답에 카드에 필요한 값이 모두 들어 있으므로 메뉴마다 추가 요청을 하지 않는다.
 */
export default function MenuCard({ menu }) {
    return (
        <article className={styles.card}>
            <div className={styles.art}>
                <MenuImage path={menu.menuImageUrl} />
            </div>
            <div className={styles.body}>
                <span className={`${styles.category} caption1`}>{menu.categoryName}</span>
                <Link to={`/menus/${menu.menuCode}`} className={`${styles.name} headline1`}>
                    {menu.menuName}
                </Link>
                <p className={`${styles.desc} label2`}>{menu.menuDescription}</p>
                <span className={`${styles.price} headline2`}>{formatPrice(menu.menuPrice)}</span>
            </div>
        </article>
    );
}

/** 로딩 중 자리를 잡아 두는 카드. 크기가 실제 카드와 같아야 레이아웃이 튀지 않는다. */
export function MenuCardSkeleton() {
    return (
        <div className={styles.card} aria-hidden="true">
            <div className={`${styles.art} ${styles.pulse}`} />
            <div className={styles.body}>
                <span className={`${styles.bar} ${styles.pulse}`} style={{ width: '48px' }} />
                <span className={`${styles.bar} ${styles.pulse}`} style={{ width: '120px', height: '20px' }} />
                <span className={`${styles.bar} ${styles.pulse}`} style={{ width: '100%' }} />
                <span className={`${styles.bar} ${styles.pulse}`} style={{ width: '64px' }} />
            </div>
        </div>
    );
}
