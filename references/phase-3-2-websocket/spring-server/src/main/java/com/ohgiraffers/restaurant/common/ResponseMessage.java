package com.ohgiraffers.restaurant.common;

import java.util.Map;

/* 설명. 정상 응답일 때 내려줄 응답 전용 템플릿 (04_spring_data_jpa chap06과 같은 형태)
 *  모든 API가 같은 모양의 JSON을 돌려주면 React 쪽에서 응답을 꺼내는 코드가 한 가지로 통일된다.
 *  ----------------------------------------------------------------------------------
 *  {
 *    "httpStatus": 200,
 *    "message": "메뉴 목록 조회 성공",
 *    "result": { "menus": [...] }      <- 실제 데이터는 항상 result 안에 담긴다
 *  }
 * */
public class ResponseMessage {

    private int httpStatus;                 // HTTP 응답 상태코드
    private String message;                 // HTTP 응답 메세지
    private Map<String, Object> result;     // HTTP 응답 데이터

    public ResponseMessage() {
    }

    public ResponseMessage(int httpStatus, String message, Map<String, Object> result) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.result = result;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "httpStatus=" + httpStatus +
                ", message='" + message + '\'' +
                ", result=" + result +
                '}';
    }
}
