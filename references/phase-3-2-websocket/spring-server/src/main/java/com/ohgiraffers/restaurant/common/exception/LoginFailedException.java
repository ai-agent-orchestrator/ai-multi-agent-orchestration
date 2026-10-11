package com.ohgiraffers.restaurant.common.exception;

/* 설명. 아이디 또는 비밀번호가 일치하지 않을 때 던지는 예외 -> 401 Unauthorized
 *  주의. "아이디가 없다"와 "비밀번호가 틀렸다"를 구분해서 알려주면
 *       어떤 아이디가 가입되어 있는지를 외부에서 알아낼 수 있다.
 *       그래서 두 경우 모두 같은 메시지로 응답한다.
 * */
public class LoginFailedException extends RuntimeException {

    public LoginFailedException(String message) {
        super(message);
    }
}
