package com.ohgiraffers.restaurant.menu.dto;

import com.ohgiraffers.restaurant.menu.domain.Menu;

import java.util.List;

/* 설명. 메뉴 상세 응답 DTO
 *  목록 응답(MenuResponse)의 값에 재료와 알레르기 목록을 더했다.
 *  관리자의 메뉴 수정 화면도 이 응답으로 기존 값을 채운다.
 * */
public class MenuDetailResponse {

    private int menuCode;
    private String menuName;
    private int menuPrice;
    private int categoryCode;
    private String categoryName;
    private String orderableStatus;
    private String menuDescription;
    private String menuIngredients;
    private String menuImageUrl;
    private List<AllergenResponse> allergens;

    public MenuDetailResponse() {}

    public static MenuDetailResponse from(Menu menu) {

        MenuDetailResponse response = new MenuDetailResponse();
        response.menuCode = menu.getMenuCode();
        response.menuName = menu.getMenuName();
        response.menuPrice = menu.getMenuPrice();
        response.categoryCode = menu.getCategory().getCategoryCode();
        response.categoryName = menu.getCategory().getCategoryName();
        response.orderableStatus = menu.getOrderableStatus();
        response.menuDescription = menu.getMenuDescription();
        response.menuIngredients = menu.getMenuIngredients();
        response.menuImageUrl = MenuResponse.toImageUrl(menu.getMenuImageName());
        response.allergens = menu.getMenuAllergens().stream()
                .map(menuAllergen -> AllergenResponse.from(menuAllergen.getAllergen()))
                .toList();

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

    public int getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getOrderableStatus() {
        return orderableStatus;
    }

    public String getMenuDescription() {
        return menuDescription;
    }

    public String getMenuIngredients() {
        return menuIngredients;
    }

    public String getMenuImageUrl() {
        return menuImageUrl;
    }

    public List<AllergenResponse> getAllergens() {
        return allergens;
    }
}
