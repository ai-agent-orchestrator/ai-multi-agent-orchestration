import styles from './CategoryTabs.module.css';

/**
 * 최상위 카테고리(식사·음료·디저트) 탭.
 * 상위 카테고리 코드를 서버에 보내면 그 아래 하위 카테고리의 메뉴까지 함께 온다.
 * value 가 null 이면 '전체'다.
 */
export default function CategoryTabs({ categories, value, onChange }) {
    // refCategoryCode 가 null 인 것이 최상위 카테고리다.
    const topCategories = categories.filter((category) => category.refCategoryCode === null);

    const tabs = [{ categoryCode: null, categoryName: '전체' }, ...topCategories];

    return (
        <div className={styles.tabs} role="tablist" aria-label="카테고리">
            {tabs.map((tab) => {
                const selected = tab.categoryCode === value;
                return (
                    <button
                        key={tab.categoryCode ?? 'all'}
                        type="button"
                        role="tab"
                        aria-selected={selected}
                        className={`${styles.tab} label1 ${selected ? styles.on : ''}`}
                        onClick={() => onChange(tab.categoryCode)}
                    >
                        {tab.categoryName}
                    </button>
                );
            })}
        </div>
    );
}
