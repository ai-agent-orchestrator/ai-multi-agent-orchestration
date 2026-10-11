import styles from './Field.module.css';

/**
 * 입력 항목 하나 (이름표 + 입력 요소 + 도움말).
 * 입력 요소 자체의 모양은 Field.module.css 의 control 클래스를 가져다 붙인다.
 *   import fieldStyles from '../components/Field.module.css';
 *   <input className={`${fieldStyles.control} body1`} ... />
 */
export default function Field({ label, htmlFor, hint, children }) {
    return (
        <div className={styles.field}>
            <label htmlFor={htmlFor} className={`${styles.label} label1`}>
                {label}
            </label>
            {children}
            {hint && <p className={`${styles.hint} caption1`}>{hint}</p>}
        </div>
    );
}
