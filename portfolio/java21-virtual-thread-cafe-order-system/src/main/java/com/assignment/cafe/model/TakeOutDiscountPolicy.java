package com.assignment.cafe.model;

/*
 * 포장 주문일 때 500원을 할인하는 정책입니다.
 * NoDiscountPolicy와 같은 DiscountPolicy 인터페이스를 구현하지만 계산 방식이 다릅니다.
 * 이것이 다형성입니다.
 */
public class TakeOutDiscountPolicy implements DiscountPolicy {
    private static final int DISCOUNT_AMOUNT = 500;

    @Override
    public int discount(int totalPrice) {
        return Math.min(DISCOUNT_AMOUNT, totalPrice);
    }

    @Override
    public String getName() {
        return "포장 할인";
    }
}
