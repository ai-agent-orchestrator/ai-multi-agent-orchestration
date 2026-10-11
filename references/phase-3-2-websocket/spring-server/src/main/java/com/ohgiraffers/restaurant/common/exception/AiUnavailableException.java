package com.ohgiraffers.restaurant.common.exception;

/* 설명. [Phase 3] AI 기능을 쓸 수 없는 상태일 때 -> 503 Service Unavailable
 *  예) 환경 변수 OPENAI_API_KEY 가 설정되지 않은 채로 서버를 실행한 경우
 *  키가 없어도 서버는 정상으로 뜨고, AI 기능만 이 예외로 막는다. (메뉴 · 주문 등 나머지 기능은 그대로 쓸 수 있다)
 * */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }
}
