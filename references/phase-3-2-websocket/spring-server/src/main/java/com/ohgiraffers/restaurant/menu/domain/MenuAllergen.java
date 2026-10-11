package com.ohgiraffers.restaurant.menu.domain;

import jakarta.persistence.*;

/* 설명. 메뉴와 알레르기의 다대다(N:M) 관계를 풀어낸 연관 엔티티
 *  테이블과 마찬가지로 복합키 대신 인조식별자(menuAllergenCode) 하나만 @Id로 둔다.
 *  그 결과 연관 엔티티도 다른 엔티티와 똑같이 @Id 하나와 @ManyToOne 두 개로 표현된다.
 * */
@Entity
@Table(name = "tbl_menu_allergen")
public class MenuAllergen {

    @Id
    @Column(name = "menu_allergen_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int menuAllergenCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_code")
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergen_code")
    private Allergen allergen;

    public MenuAllergen() {}

    public MenuAllergen(Menu menu, Allergen allergen) {
        this.menu = menu;
        this.allergen = allergen;
    }

    public int getMenuAllergenCode() {
        return menuAllergenCode;
    }

    public Menu getMenu() {
        return menu;
    }

    public Allergen getAllergen() {
        return allergen;
    }
}
