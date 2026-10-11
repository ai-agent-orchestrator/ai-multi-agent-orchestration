package com.ohgiraffers.restaurant.common;

/* 설명. 예외 상황일 때 내려줄 응답 전용 템플릿 (04_spring_data_jpa chap06과 같은 형태)
 *  정상 응답은 ResponseMessage, 예외 응답은 ErrorResponse로 형태를 나눠둔다.
 *  React는 HTTP 상태 코드만 보고 어떤 모양의 JSON이 올지 미리 알 수 있다.
 *  ----------------------------------------------------------------------------------
 *  - code        : 애플리케이션이 정한 자체 에러 코드 (HTTP 상태 코드와는 별개)
 *  - description : 어떤 상황인지에 대한 짧은 설명
 *  - detail      : 실제로 발생한 예외 메시지 등 구체적인 내용
 * */
public class ErrorResponse {

    private String code;
    private String description;
    private String detail;

    public ErrorResponse() {}

    public ErrorResponse(String code, String description, String detail) {
        this.code = code;
        this.description = description;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
