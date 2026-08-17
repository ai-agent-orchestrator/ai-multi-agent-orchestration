package com.assignment.cafe.model;

/*
 * 요구사항 5번(chap15 enum): 메뉴 분류를 표현하는 enum입니다.
 *
 * 메뉴 분류는 COFFEE, TEA, ADE, DESSERT 중 하나로 제한됩니다.
 * 문자열로 "커피", "cofee"처럼 직접 입력받으면 오타가 생길 수 있지만,
 * enum을 사용하면 정해진 값만 사용할 수 있습니다.
 */
public enum MenuCategory {
    COFFEE("커피"),
    TEA("차"),
    ADE("에이드"),
    DESSERT("디저트");

    private final String displayName;

    MenuCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
