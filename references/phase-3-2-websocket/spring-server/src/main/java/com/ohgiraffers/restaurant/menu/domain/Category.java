package com.ohgiraffers.restaurant.menu.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_category")
public class Category {

    @Id
    @Column(name = "category_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int categoryCode;

    @Column(name = "category_name")
    private String categoryName;

    /* 설명. 셀프 참조 (상위 카테고리)
     *  상위 카테고리가 없는 최상위 카테고리(식사, 음료, 디저트)는 이 값이 null이다.
     *  ----------------------------------------------------------------------------------
     *  이 프로젝트의 @ManyToOne은 모두 지연 로딩(LAZY)으로 설정하고,
     *  함께 필요한 경우에만 Repository에서 페치 조인(@EntityGraph, JOIN FETCH)으로 한 번에 가져온다.
     *  (04_spring_data_jpa에서 확인한 N+1 문제를 피하기 위한 방식이다)
     * */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_category_code")
    private Category parentCategory;

    public Category() {}

    public int getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    @Override
    public String toString() {
        return "Category{" +
                "categoryCode=" + categoryCode +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
