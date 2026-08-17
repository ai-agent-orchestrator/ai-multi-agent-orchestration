package com.assignment.cafe.model;

/*
 * 요구사항 2번: 주문 안에 들어가는 개별 주문 항목 모델입니다.
 *
 * 예를 들어 "아메리카노 2잔"은 하나의 OrderItem입니다.
 * CafeOrder는 여러 OrderItem을 List로 가지고 있으므로,
 * 한 주문에 여러 메뉴를 담을 수 있습니다.
 */
public class OrderItem {
    private final CafeMenu menu;
    private int quantity;

    public OrderItem(CafeMenu menu, int quantity) {
        this.menu = menu;
        changeQuantity(quantity);
    }

    public CafeMenu getMenu() {
        return menu;
    }

    public int getQuantity() {
        return quantity;
    }

    public void changeQuantity(int quantity) {
        /*
         * 요구사항 8번: 잘못된 입력 방어
         * 수량이 0개나 음수이면 정상 주문이 아니므로 예외를 발생시킵니다.
         */
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        this.quantity = quantity;
    }

    public int getSubtotal() {
        // 메뉴 가격과 수량을 곱해서 이 항목의 소계를 계산합니다.
        return menu.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%s x %d개 = %,d원", menu.getName(), quantity, getSubtotal());
    }
}
