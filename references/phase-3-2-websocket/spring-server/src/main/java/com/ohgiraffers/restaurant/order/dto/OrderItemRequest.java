package com.ohgiraffers.restaurant.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* 설명. 주문할 메뉴 한 줄 (메뉴코드 + 수량)
 *  가격은 받지 않는다. 가격은 서버가 DB의 메뉴 가격으로 계산한다.
 * */
public class OrderItemRequest {

    @NotNull(message = "메뉴 코드는 필수입니다.")
    private Integer menuCode;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer orderAmount;

    public OrderItemRequest() {}

    public Integer getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(Integer menuCode) {
        this.menuCode = menuCode;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }
}
