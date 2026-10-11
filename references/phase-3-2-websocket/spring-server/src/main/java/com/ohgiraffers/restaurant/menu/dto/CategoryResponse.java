package com.ohgiraffers.restaurant.menu.dto;

import com.ohgiraffers.restaurant.menu.domain.Category;

/* 설명. 카테고리 응답 DTO
 *  상위 카테고리는 객체 대신 코드(refCategoryCode)로만 내려준다. 최상위 카테고리는 null이다.
 *  React는 refCategoryCode가 null인 것만 골라 상위 탭(식사·음료·디저트)을 만들 수 있다.
 * */
public class CategoryResponse {

    private int categoryCode;
    private String categoryName;
    private Integer refCategoryCode;

    public CategoryResponse() {}

    public CategoryResponse(int categoryCode, String categoryName, Integer refCategoryCode) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.refCategoryCode = refCategoryCode;
    }

    public static CategoryResponse from(Category category) {

        Category parent = category.getParentCategory();

        /* 설명. 지연 로딩 프록시에서 식별자(@Id)를 꺼내는 것은 추가 SELECT 없이 가능하다.
         *  프록시는 처음부터 자신의 식별자 값을 알고 있기 때문이다.
         * */
        return new CategoryResponse(
                category.getCategoryCode(),
                category.getCategoryName(),
                parent != null ? parent.getCategoryCode() : null
        );
    }

    public int getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getRefCategoryCode() {
        return refCategoryCode;
    }
}
