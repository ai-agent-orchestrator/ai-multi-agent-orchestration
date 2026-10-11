import styles from './AllergenChips.module.css';

/** 메뉴 상세의 알레르기 목록. 없으면 '없음'을 보여준다. */
export default function AllergenChips({ allergens }) {
    if (allergens.length === 0) {
        return <p className={`${styles.none} body2`}>표시 대상 알레르기 성분이 없습니다.</p>;
    }
    return (
        <ul className={styles.list}>
            {allergens.map((allergen) => (
                <li key={allergen.allergenCode} className={`${styles.chip} label2`}>
                    {allergen.allergenName}
                </li>
            ))}
        </ul>
    );
}
