import { create } from 'zustand';

/*
 * 헤더의 장바구니 배지에 보여줄 개수(담은 수량의 합).
 *
 * 장바구니 목록은 서버(DB)에 있고, 장바구니 화면이 받아 와서 직접 가진다.
 * 여러 화면(헤더 · 메뉴 상세 · 장바구니)이 함께 봐야 하는 값은 '개수' 뿐이라 이것만 스토어에 둔다.
 * persist 를 쓰지 않는다. 새로고침하면 헤더가 서버에서 다시 받아 온다.
 */
export const useCartStore = create((set) => ({
    count: 0,

    // 서버가 돌려준 장바구니(cartItems)로 개수를 다시 센다.
    setCountFrom: (cartItems) =>
        set({ count: cartItems.reduce((sum, item) => sum + item.cartAmount, 0) }),

    reset: () => set({ count: 0 }),
}));
