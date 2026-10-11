import styles from './Button.module.css';

/**
 * Montage 의 버튼 기하를 그대로 따른다.
 *   large  모서리 12px / 여백 12px 28px / label1
 *   medium 모서리 10px / 여백  9px 20px / label1
 *   small  모서리  8px / 여백  7px 14px / label2
 */
export default function Button({ variant = 'primary', size = 'medium', ...props }) {
    return (
        <button
            type="button"
            className={`${styles.button} ${styles[variant]} ${styles[size]} ${
                size === 'small' ? 'label2' : 'label1'
            }`}
            {...props}
        />
    );
}
