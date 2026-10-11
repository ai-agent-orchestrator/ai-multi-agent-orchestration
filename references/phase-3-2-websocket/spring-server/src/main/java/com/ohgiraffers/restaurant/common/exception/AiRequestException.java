package com.ohgiraffers.restaurant.common.exception;

/* 설명. [Phase 3] OpenAI 호출이 실패했을 때 -> 502 Bad Gateway
 *  우리 서버는 정상인데 우리가 호출한 다른 서버(OpenAI)가 실패했다는 뜻으로 502 를 쓴다.
 *  예) API 키가 틀림(401), 충전 잔액 부족 · 요청 한도 초과(429), OpenAI 서버 오류(5xx)
 * */
public class AiRequestException extends RuntimeException {

    public AiRequestException(String message) {
        super(message);
    }
}
