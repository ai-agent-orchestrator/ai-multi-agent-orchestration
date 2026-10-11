package com.ohgiraffers.restaurant.menu.dto;

import com.ohgiraffers.restaurant.menu.domain.Menu;

/* 설명. 메뉴 목록 응답 DTO
 *  목록 화면(메뉴 카드)에 필요한 값만 담는다. 알레르기처럼 상세 화면에서만 필요한 값은 MenuDetailResponse에 있다.
 *  ----------------------------------------------------------------------------------
 *  menuImageUrl은 DB의 파일명 앞에 /uploads/menus/ 를 붙인 경로다. (이미지가 없으면 null)
 *  React에서는 서버 주소를 앞에 붙여 사용한다.
 *  예) <img src={`http://localhost:8080${menu.menuImageUrl}`} />
 * */
public class MenuResponse {

    private int menuCode;
    private String menuName;
    private int menuPrice;
    private int categoryCode;
    private String categoryName;
    private String orderableStatus;
    private String menuDescription;
    private String menuImageUrl;

    public MenuResponse() {}

    public static MenuResponse from(Menu menu) {

        MenuResponse response = new MenuResponse();
        response.menuCode = menu.getMenuCode();
        response.menuName = menu.getMenuName();
        response.menuPrice = menu.getMenuPrice();
        response.categoryCode = menu.getCategory().getCategoryCode();
        response.categoryName = menu.getCategory().getCategoryName();
        response.orderableStatus = menu.getOrderableStatus();
        response.menuDescription = menu.getMenuDescription();
        response.menuImageUrl = toImageUrl(menu.getMenuImageName());

        return response;
    }

    /* 설명. 저장된 파일명을 브라우저에서 접근할 수 있는 경로로 바꾼다. (WebConfig 참고) */
    public static String toImageUrl(String menuImageName) {
        return menuImageName != null ? "/uploads/menus/" + menuImageName : null;
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

    public String getMenuImageUrl() {
        return menuImageUrl;
    }
}
