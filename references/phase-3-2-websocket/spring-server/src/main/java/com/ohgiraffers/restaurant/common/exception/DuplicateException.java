package com.ohgiraffers.restaurant.common.exception;

/* 설명. 이미 존재하는 값으로 새 자원을 만들려고 할 때 던지는 예외 -> 409 Conflict
 *  예) 이미 가입된 아이디로 회원가입을 시도한 경우
 * */
public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }
}
