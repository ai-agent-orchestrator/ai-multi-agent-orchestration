package com.ohgiraffers.restaurant.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* 설명. 장바구니 수량 변경 요청 본문(JSON)
 *  { "cartAmount": 3 }
 *  바꿀 메뉴는 주소(/cart/items/{menuCode})로 정한다.
 * */
public class CartAmountRequest {

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Max(value = 99, message = "수량은 99개 이하여야 합니다.")
    private Integer cartAmount;

    public CartAmountRequest() {}

    public Integer getCartAmount() {
        return cartAmount;
    }

    public void setCartAmount(Integer cartAmount) {
        this.cartAmount = cartAmount;
    }
}
