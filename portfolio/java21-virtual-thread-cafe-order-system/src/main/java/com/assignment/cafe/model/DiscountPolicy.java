package com.assignment.cafe.model;

/*
 * 요구사항 7번(chap09 다형성): 할인 정책을 표현하는 인터페이스입니다.
 *
 * 인터페이스는 "이런 기능을 반드시 가져야 한다"는 약속입니다.
 * NoDiscountPolicy와 TakeOutDiscountPolicy는 서로 다른 할인 방식을 가지지만,
 * 둘 다 DiscountPolicy 타입으로 사용할 수 있습니다.
 *
 * 덕분에 CafeOrder는 할인 정책이 정확히 어떤 클래스인지 몰라도
 * discount(totalPrice) 메소드만 호출하면 됩니다.
 */
public interface DiscountPolicy {
    int discount(int totalPrice);

    String getName();
}
