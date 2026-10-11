import { api, resultOf } from './client.js';

/*
 * 장바구니 API. 장바구니는 서버(DB)에 회원별로 저장된다.
 * 담기 · 수량 변경 · 빼기는 모두 '바뀐 뒤의 장바구니 전체(cartItems)'를 돌려준다.
 *
 * [Phase 2] 주소에 memberCode 가 없다. 장바구니의 주인은 client.js 의 인터셉터가 붙인 토큰으로 서버가 정한다.
 * Phase 1 에서 화면들이 memberCode 를 넘기지 않게 해 두었으므로, 바뀌는 곳은 이 파일뿐이다.
 */
const CART_URL = '/api/members/me/cart';

// CartController.findCart()   GET /api/members/me/cart   (@AuthenticationPrincipal)
export async function fetchCart(signal) {
    const response = await api.get(CART_URL, { signal });
    return resultOf(response).cartItems;
}

// CartController.addCartItem()   POST /api/members/me/cart/items   (@RequestBody CartItemRequest)
export async function addCartItem({ menuCode, cartAmount }) {
    const response = await api.post(`${CART_URL}/items`, { menuCode, cartAmount });
    return resultOf(response).cartItems;
}

// CartController.changeCartItem()   PUT /api/members/me/cart/items/{menuCode}   (@RequestBody CartAmountRequest)
export async function changeCartItem(menuCode, cartAmount) {
    const response = await api.put(`${CART_URL}/items/${menuCode}`, { cartAmount });
    return resultOf(response).cartItems;
}

// CartController.removeCartItem()   DELETE /api/members/me/cart/items/{menuCode}
export async function removeCartItem(menuCode) {
    const response = await api.delete(`${CART_URL}/items/${menuCode}`);
    return resultOf(response).cartItems;
}
