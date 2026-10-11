package com.ohgiraffers.restaurant.cart.dto;

import com.ohgiraffers.restaurant.cart.domain.Cart;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import com.ohgiraffers.restaurant.menu.dto.MenuResponse;

/* 설명. 장바구니에 담긴 메뉴 한 줄의 응답
 *  장바구니 화면이 그리는 데 필요한 메뉴 정보(이름, 가격, 이미지, 주문 가능 여부)를 함께 담는다.
 *  orderableStatus 가 'N' 이면 담아 둔 사이에 관리자가 품절 처리한 것이다. (주문하면 400)
 * */
public class CartItemResponse {

    private int cartCode;
    private int menuCode;
    private String menuName;
    private int menuPrice;
    private String orderableStatus;
    private String menuImageUrl;
    private int cartAmount;

    public static CartItemResponse from(Cart cart) {
        Menu menu = cart.getMenu();

        CartItemResponse response = new CartItemResponse();
        response.cartCode = cart.getCartCode();
        response.menuCode = menu.getMenuCode();
        response.menuName = menu.getMenuName();
        response.menuPrice = menu.getMenuPrice();
        response.orderableStatus = menu.getOrderableStatus();
        response.menuImageUrl = MenuResponse.toImageUrl(menu.getMenuImageName());
        response.cartAmount = cart.getCartAmount();
        return response;
    }

    public int getCartCode() {
        return cartCode;
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

    public String getOrderableStatus() {
        return orderableStatus;
    }

    public String getMenuImageUrl() {
        return menuImageUrl;
    }

    public int getCartAmount() {
        return cartAmount;
    }
}
