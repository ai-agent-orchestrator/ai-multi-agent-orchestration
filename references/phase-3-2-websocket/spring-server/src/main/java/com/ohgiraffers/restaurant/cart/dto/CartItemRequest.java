package com.ohgiraffers.restaurant.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* 설명. 장바구니 담기 요청 본문(JSON)
 *  { "menuCode": 1, "cartAmount": 2 }
 * */
public class CartItemRequest {

    @NotNull(message = "메뉴 코드는 필수입니다.")
    private Integer menuCode;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Max(value = 99, message = "수량은 99개 이하여야 합니다.")
    private Integer cartAmount;

    public CartItemRequest() {}

    public Integer getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(Integer menuCode) {
        this.menuCode = menuCode;
    }

    public Integer getCartAmount() {
        return cartAmount;
    }

    public void setCartAmount(Integer cartAmount) {
        this.cartAmount = cartAmount;
    }
}
