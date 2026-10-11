package com.ohgiraffers.restaurant.common.exception;

/* 설명. 조회하려는 자원(회원, 메뉴, 주문 등)이 존재하지 않을 때 던지는 예외 -> 404 Not Found */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
