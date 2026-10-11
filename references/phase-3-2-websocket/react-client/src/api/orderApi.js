import { api, resultOf } from './client.js';

/*
 * OrderController.placeOrder()   POST /api/orders   (@AuthenticationPrincipal + @RequestBody OrderRequest)
 *
 * 가격은 보내지 않는다. 서버가 DB 의 메뉴 가격으로 계산한다.
 *
 * [Phase 2] memberCode 를 보내지 않는다. 주문한 사람은 서버가 토큰에서 꺼낸다.
 */
export async function placeOrder({ paymentType, items }) {
    const response = await api.post('/api/orders', {
        paymentType,
        items: items.map((item) => ({ menuCode: item.menuCode, orderAmount: item.cartAmount })),
    });
    return resultOf(response).order;
}

// OrderController.findMyOrders()   GET /api/members/me/orders   (@AuthenticationPrincipal)
// [Phase 2] 경로에 memberCode 가 없다. 토큰의 주인이 곧 '나'다.
export async function fetchMyOrders(signal) {
    const response = await api.get('/api/members/me/orders', { signal });
    return resultOf(response).orders;
}
