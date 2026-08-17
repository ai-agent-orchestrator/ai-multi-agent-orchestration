package com.assignment.cafe.model;

/*
 * 요구사항 2번: 카페 메뉴 한 개를 표현하는 모델 클래스입니다.
 *
 * 필드는 private으로 숨기고 getId(), getName() 같은 메소드로만 읽게 했습니다.
 * 메뉴명이나 가격을 외부에서 마음대로 바꾸지 못하게 final로 선언했습니다.
 */
public class CafeMenu {
    private final int id;
    private final String name;
    private final MenuCategory category;
    private final int price;

    public CafeMenu(int id, String name, MenuCategory category, int price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s / %s / %,d원", id, name, category.getDisplayName(), price);
    }
}
