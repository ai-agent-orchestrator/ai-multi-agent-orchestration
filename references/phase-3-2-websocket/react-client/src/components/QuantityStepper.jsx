import styles from './QuantityStepper.module.css';

/** 수량 조절. 1 아래로는 내려가지 않는다. */
export default function QuantityStepper({ value, onChange, max = 99 }) {
    return (
        <div className={styles.stepper}>
            <button
                type="button"
                className={styles.button}
                onClick={() => onChange(value - 1)}
                disabled={value <= 1}
                aria-label="수량 줄이기"
            >
                -
            </button>
            <span className={`${styles.value} label1`} aria-live="polite">
                {value}
            </span>
            <button
                type="button"
                className={styles.button}
                onClick={() => onChange(value + 1)}
                disabled={value >= max}
                aria-label="수량 늘리기"
            >
                +
            </button>
        </div>
    );
}
