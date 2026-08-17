package com.assignment.cafe.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(int id) {
        super("주문번호 " + id + "번을 찾을 수 없습니다.");
    }
}
