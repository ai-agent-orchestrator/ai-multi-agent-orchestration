import styles from './StatusBlock.module.css';

/**
 * 빈 상태와 오류 상태가 같은 틀을 쓴다.
 * 둘 다 "무엇이 없는지"로 끝내지 않고 다음 행동(action)을 함께 둔다.
 */
export default function StatusBlock({ tone = 'empty', title, description, action }) {
    return (
        <div className={`${styles.block} ${tone === 'error' ? styles.error : ''}`}>
            {tone === 'error' ? <ErrorIcon /> : <SearchIcon />}
            <div className={styles.text}>
                <p className="headline1">{title}</p>
                {description && <p className={`${styles.desc} body2`}>{description}</p>}
            </div>
            {action}
        </div>
    );
}

function SearchIcon() {
    return (
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <circle cx="17" cy="17" r="12" stroke="currentColor" strokeWidth="2" />
            <path d="M26 26 L35 35 M13 17 L21 17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
    );
}

function ErrorIcon() {
    return (
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <circle cx="20" cy="20" r="15" stroke="currentColor" strokeWidth="2" />
            <path d="M20 12 L20 22 M20 27 L20 28" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
    );
}
