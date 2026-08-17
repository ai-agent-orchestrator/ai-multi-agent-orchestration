package com.assignment.cafe.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * 요구사항 2번(chap06 클래스와 객체): 데이터를 표현하는 모델 클래스입니다.
 *
 * CafeOrder는 "카페 주문 한 건"을 표현합니다.
 * 주문번호, 고객명, 주문 항목 목록, 주문 상태, 할인 정책, 주문 시간을 필드로 가집니다.
 *
 * 모든 필드는 private으로 선언했습니다.
 * 외부 클래스가 필드에 직접 접근하지 못하게 막고,
 * getId(), changeCustomerName() 같은 메소드로만 접근하게 하기 위해서입니다.
 * 이것을 캡슐화라고 합니다.
 */
public class CafeOrder {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final int id;
    private volatile String customerName;
    private final List<OrderItem> items;
    private volatile OrderStatus status;
    private DiscountPolicy discountPolicy;
    private final LocalDateTime orderedAt;

    public CafeOrder(int id, String customerName, List<OrderItem> items, DiscountPolicy discountPolicy) {
        this.id = id;
        this.customerName = customerName;
        /*
         * 전달받은 List를 그대로 저장하지 않고 새 ArrayList로 복사합니다.
         * 외부에서 원본 List를 수정해도 주문 내부 데이터가 갑자기 바뀌지 않게 하기 위한 보호 장치입니다.
         */
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.WAITING;
        this.discountPolicy = discountPolicy;
        this.orderedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void changeCustomerName(String customerName) {
        /*
         * 요구사항 8번 예외 처리와도 연결됩니다.
         * 잘못된 고객명이 들어오면 조용히 저장하지 않고 예외를 발생시켜
         * Controller/Application에서 사용자에게 오류 메시지를 보여주게 합니다.
         */
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("고객명은 비워둘 수 없습니다.");
        }

        this.customerName = customerName.trim();
    }

    public List<OrderItem> getItems() {
        /*
         * 주문 항목 List를 그대로 돌려주면 외부에서 clear(), add()로 마음대로 바꿀 수 있습니다.
         * unmodifiableList는 읽기 전용 목록을 반환해서 객체의 내부 상태를 보호합니다.
         */
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void changeDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public int getOriginalPrice() {
        /*
         * Stream API 활용 예시입니다.
         * items.stream()으로 주문 항목을 흐름처럼 만들고,
         * mapToInt(OrderItem::getSubtotal)로 각 항목의 소계를 숫자로 바꾼 뒤,
         * sum()으로 전체 금액을 더합니다.
         */
        return items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();
    }

    public int getDiscountAmount() {
        return discountPolicy.discount(getOriginalPrice());
    }

    public int getFinalPrice() {
        return getOriginalPrice() - getDiscountAmount();
    }

    public boolean containsMenuName(String keyword) {
        /*
         * 요구사항 6번: Stream API 검색 기능에 사용됩니다.
         * 주문 항목 중 하나라도 메뉴명에 keyword가 포함되면 true를 반환합니다.
         * anyMatch는 조건을 만족하는 요소가 하나라도 있는지 검사합니다.
         */
        return items.stream()
                .anyMatch(item -> item.getMenu().getName().toLowerCase().contains(keyword.toLowerCase()));
    }

    @Override
    public String toString() {
        /*
         * 주문 항목 여러 개를 "아메리카노 x 2개, 쿠키 x 1개" 같은 문자열로 합칩니다.
         * reduce는 여러 값을 하나의 결과로 누적할 때 사용합니다.
         */
        String itemText = items.stream()
                .map(OrderItem::toString)
                .reduce((left, right) -> left + ", " + right)
                .orElse("주문 항목 없음");

        return String.format(
                "주문번호: %d | 고객명: %s | 상태: %s | 주문: %s | 할인: %s %,d원 | 결제금액: %,d원 | 시간: %s",
                id,
                customerName,
                status.getDescription(),
                itemText,
                discountPolicy.getName(),
                getDiscountAmount(),
                getFinalPrice(),
                orderedAt.format(FORMATTER)
        );
    }
}
