import { imageUrl } from '../api/client.js';
import styles from './MenuImage.module.css';

/**
 * 메뉴 이미지. 서버가 준 경로(menuImageUrl) 앞에 서버 주소를 붙여 보여준다.
 * 이미지가 없는 메뉴(null)는 같은 크기의 빈 자리를 대신 그린다.
 * 크기는 감싸는 쪽에서 정한다. 이 컴포넌트는 부모의 가로·세로를 꽉 채운다.
 *
 * compact 를 켜면 빈 자리에 글자를 쓰지 않는다. (주문 내역의 작은 썸네일처럼 글자가 들어갈 자리가 없을 때)
 */
export default function MenuImage({ path, alt = '', compact = false }) {
    const src = imageUrl(path);

    if (!src) {
        return (
            <div className={`${styles.empty} caption1`} role="img" aria-label="이미지 준비 중">
                {!compact && '이미지 준비 중'}
            </div>
        );
    }
    return <img className={styles.image} src={src} alt={alt} loading="lazy" />;
}
