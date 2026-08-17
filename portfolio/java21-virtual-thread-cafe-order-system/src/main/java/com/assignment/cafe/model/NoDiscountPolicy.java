package com.assignment.cafe.model;

/*
 * 매장 주문처럼 할인이 없는 경우의 정책입니다.
 * DiscountPolicy 인터페이스를 구현하므로 TakeOutDiscountPolicy와 같은 타입으로 다룰 수 있습니다.
 */
public class NoDiscountPolicy implements DiscountPolicy {
    @Override
    public int discount(int totalPrice) {
        return 0;
    }

    @Override
    public String getName() {
        return "할인 없음";
    }
}
