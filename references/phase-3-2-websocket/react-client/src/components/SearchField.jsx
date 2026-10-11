import { useEffect, useId, useState } from 'react';
import styles from './SearchField.module.css';

/**
 * 입력을 바로 위로 올리지 않고 debounce 로 늦춘다.
 * 타자 한 글자마다 목록이 다시 그려지면 눈이 피로하고, 요청도 늘어난다.
 */
export default function SearchField({ value, onChange, label = '검색어', placeholder = '검색어를 입력하세요', delay = 250 }) {
    const id = useId();
    const [draft, setDraft] = useState(value);
    const [synced, setSynced] = useState(value);

    // 바깥에서 값이 바뀌면(초기화 버튼, 뒤로가기) 입력창도 따라간다.
    // effect 가 아니라 렌더 중에 맞춘다. effect 로 하면 한 번 그린 뒤 다시 그리게 되고,
    // React 가 권하지 않는 방식이다 (react.dev "You Might Not Need an Effect").
    if (value !== synced) {
        setSynced(value);
        setDraft(value);
    }

    useEffect(() => {
        if (draft === value) return;
        const timer = setTimeout(() => onChange(draft), delay);
        return () => clearTimeout(timer);
    }, [draft, value, delay, onChange]);

    return (
        <div className={styles.field}>
            <svg className={styles.icon} width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
                <circle cx="9" cy="9" r="6" fill="none" stroke="currentColor" strokeWidth="1.75" />
                <path d="M13.5 13.5 L17 17" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
            </svg>

            <label htmlFor={id} className={styles.srOnly}>
                {label}
            </label>
            <input
                id={id}
                type="search"
                className={`${styles.input} body1`}
                placeholder={placeholder}
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
            />

            {draft !== '' && (
                <button
                    type="button"
                    className={styles.clear}
                    onClick={() => setDraft('')}
                    aria-label="검색어 지우기"
                >
                    <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true">
                        <path
                            d="M3 3 L9 9 M9 3 L3 9"
                            stroke="currentColor"
                            strokeWidth="1.75"
                            strokeLinecap="round"
                        />
                    </svg>
                </button>
            )}
        </div>
    );
}
