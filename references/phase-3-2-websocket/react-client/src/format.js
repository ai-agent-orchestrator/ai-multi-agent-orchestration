/** 12000 -> '12,000원' */
export function formatPrice(price) {
    return `${price.toLocaleString('ko-KR')}원`;
}

/** '2026-09-18T12:30:00' -> '2026. 9. 18. 오후 12:30' */
export function formatDateTime(isoString) {
    return new Date(isoString).toLocaleString('ko-KR', {
        dateStyle: 'medium',
        timeStyle: 'short',
    });
}

export const PAYMENT_TYPE_LABEL = {
    CARD: '카드',
    CASH: '현금',
};
