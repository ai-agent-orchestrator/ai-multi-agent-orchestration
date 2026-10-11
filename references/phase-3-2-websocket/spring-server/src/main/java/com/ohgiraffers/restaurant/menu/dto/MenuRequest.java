package com.ohgiraffers.restaurant.menu.dto;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/* 설명. 관리자의 메뉴 등록·수정 요청 DTO
 *  ----------------------------------------------------------------------------------
 *  이미지 파일과 함께 보내야 하므로 JSON이 아니라 multipart/form-data 형식으로 받는다.
 *  컨트롤러에서는 @RequestBody 대신 @ModelAttribute로 받으며,
 *  form-data의 각 항목 이름이 이 클래스의 필드 이름과 같으면 setter를 통해 자동으로 채워진다.
 *  ----------------------------------------------------------------------------------
 *  React 쪽 예시)
 *    const formData = new FormData();
 *    formData.append('menuName', '열무김치라떼');
 *    formData.append('allergenCodes', 2);      // 같은 이름으로 여러 번 append하면 List로 받는다
 *    formData.append('allergenCodes', 9);
 *    formData.append('menuImage', file);       // 이미지 파일은 컨트롤러의 MultipartFile 파라미터로 받는다
 *  (React 19의 form Actions를 쓰면 <form>이 만들어준 FormData를 그대로 보낼 수도 있다)
 * */
public class MenuRequest {

    @NotBlank(message = "메뉴명은 필수입니다.")
    @Size(max = 30, message = "메뉴명은 30자 이하로 입력해주세요.")
    private String menuName;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer menuPrice;

    @NotNull(message = "카테고리는 필수입니다.")
    private Integer categoryCode;

    @NotBlank(message = "주문 가능 상태는 필수입니다.")
    @Pattern(regexp = "[YN]", message = "주문 가능 상태는 Y 또는 N이어야 합니다.")
    private String orderableStatus;

    @Size(max = 500, message = "상세설명은 500자 이하로 입력해주세요.")
    private String menuDescription;

    @Size(max = 300, message = "재료는 300자 이하로 입력해주세요.")
    private String menuIngredients;

    /* 설명. 알레르기가 하나도 없는 메뉴는 이 값을 보내지 않아도 된다. (빈 목록으로 처리) */
    private List<Integer> allergenCodes = new ArrayList<>();

    public MenuRequest() {}

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Integer getMenuPrice() {
        return menuPrice;
    }

    public void setMenuPrice(Integer menuPrice) {
        this.menuPrice = menuPrice;
    }

    public Integer getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(Integer categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getOrderableStatus() {
        return orderableStatus;
    }

    public void setOrderableStatus(String orderableStatus) {
        this.orderableStatus = orderableStatus;
    }

    public String getMenuDescription() {
        return menuDescription;
    }

    public void setMenuDescription(String menuDescription) {
        this.menuDescription = menuDescription;
    }

    public String getMenuIngredients() {
        return menuIngredients;
    }

    public void setMenuIngredients(String menuIngredients) {
        this.menuIngredients = menuIngredients;
    }

    public List<Integer> getAllergenCodes() {
        return allergenCodes;
    }

    public void setAllergenCodes(List<Integer> allergenCodes) {
        this.allergenCodes = allergenCodes != null ? allergenCodes : new ArrayList<>();
    }
}
