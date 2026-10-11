import styles from './Pagination.module.css';

/**
 * 현재 페이지 주변만 보여주고 나머지는 … 로 접는다.
 * 1 … 4 [5] 6 … 68 처럼.
 */
function pageItems(current, last) {
    if (last <= 7) {
        return Array.from({ length: last }, (_, i) => ({ page: i + 1, key: i + 1 }));
    }

    const around = [current - 1, current, current + 1].filter((n) => n > 1 && n < last);
    const pages = [...new Set([1, ...around, last])].sort((a, b) => a - b);

    const out = [];
    let prev = 0;
    for (const n of pages) {
        if (n - prev > 1) out.push({ gap: true, key: `gap-${n}` });
        out.push({ page: n, key: n });
        prev = n;
    }
    return out;
}

export default function Pagination({ page, lastPage, onChange }) {
    if (lastPage <= 1) return null;
    const items = pageItems(page, lastPage);

    return (
        <nav className={styles.nav} aria-label="페이지">
            <button
                type="button"
                className={styles.arrow}
                onClick={() => onChange(page - 1)}
                disabled={page <= 1}
                aria-label="이전 페이지"
            >
                <Chevron dir="left" />
            </button>

            {items.map((it) =>
                it.gap ? (
                    <span key={it.key} className={`${styles.gap} body1`} aria-hidden="true">
                        …
                    </span>
                ) : (
                    <button
                        key={it.key}
                        type="button"
                        className={`${styles.page} label1 ${it.page === page ? styles.current : ''}`}
                        onClick={() => onChange(it.page)}
                        aria-current={it.page === page ? 'page' : undefined}
                    >
                        {it.page}
                    </button>
                ),
            )}

            <button
                type="button"
                className={styles.arrow}
                onClick={() => onChange(page + 1)}
                disabled={page >= lastPage}
                aria-label="다음 페이지"
            >
                <Chevron dir="right" />
            </button>
        </nav>
    );
}

function Chevron({ dir }) {
    return (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path
                d={dir === 'left' ? 'M10 3 L5 8 L10 13' : 'M6 3 L11 8 L6 13'}
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
        </svg>
    );
}
