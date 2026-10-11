package com.ohgiraffers.restaurant.order.dto;

import com.ohgiraffers.restaurant.menu.dto.MenuResponse;
import com.ohgiraffers.restaurant.order.domain.OrderMenu;

/* 설명. 주문 내역 안의 메뉴 한 줄
 *  주의. menuPrice는 주문 당시의 가격이 아니라 '현재' 메뉴 가격이다.
 *       관리자가 나중에 가격을 바꾸면 이 값도 바뀐다. (주문 당시 금액은 주문의 totalOrderPrice에 남아 있다)
 * */
public class OrderMenuResponse {

    private int menuCode;
    private String menuName;
    private int menuPrice;
    private int orderAmount;
    private String menuImageUrl;

    public OrderMenuResponse() {}

    public static OrderMenuResponse from(OrderMenu orderMenu) {

        OrderMenuResponse response = new OrderMenuResponse();
        response.menuCode = orderMenu.getMenu().getMenuCode();
        response.menuName = orderMenu.getMenu().getMenuName();
        response.menuPrice = orderMenu.getMenu().getMenuPrice();
        response.orderAmount = orderMenu.getOrderAmount();
        response.menuImageUrl = MenuResponse.toImageUrl(orderMenu.getMenu().getMenuImageName());

        return response;
    }

    public int getMenuCode() {
        return menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getMenuPrice() {
        return menuPrice;
    }

    public int getOrderAmount() {
        return orderAmount;
    }

    public String getMenuImageUrl() {
        return menuImageUrl;
    }
}
