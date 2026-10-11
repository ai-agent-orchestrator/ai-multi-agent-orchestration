package com.ohgiraffers.restaurant.ai.dto;

/* 설명. [Phase 3-2] 관리자 화면이 WebSocket 으로 보내는 메시지 (JSON 한 덩어리)
 *  ----------------------------------------------------------------------------------
 *  WebSocket 에는 HTTP 처럼 주소(URL)나 메서드(GET/POST)가 없다. 연결 하나로 모든 메시지가 오가므로,
 *  메시지마다 type 을 적어 '무엇을 해 달라는 메시지인지'를 구분한다.
 *  ----------------------------------------------------------------------------------
 *  type    | 쓰는 필드                                        | 뜻
 *  auth    | token                                            | 연결 직후 한 번. 토큰으로 관리자인지 확인한다
 *  draft   | menuName, categoryName, menuPrice, menuIngredients | 이 메뉴의 설명 초안을 써 달라
 *  revise  | instruction                                      | 방금 쓴 초안을 이렇게 고쳐 달라 (예: 더 짧게)
 *  cancel  | -                                                | 만들고 있는 초안을 멈춰 달라
 *  image   | menuName, categoryName, menuIngredients, menuDescription | [선택] 메뉴 사진을 만들어 달라
 * */
public class MenuDraftMessage {

    private String type;
    private String token;
    private String menuName;
    private String categoryName;
    private Integer menuPrice;
    private String menuIngredients;
    private String instruction;
    private String menuDescription;

    public MenuDraftMessage() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getMenuPrice() {
        return menuPrice;
    }

    public void setMenuPrice(Integer menuPrice) {
        this.menuPrice = menuPrice;
    }

    public String getMenuIngredients() {
        return menuIngredients;
    }

    public void setMenuIngredients(String menuIngredients) {
        this.menuIngredients = menuIngredients;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getMenuDescription() {
        return menuDescription;
    }

    public void setMenuDescription(String menuDescription) {
        this.menuDescription = menuDescription;
    }
}
