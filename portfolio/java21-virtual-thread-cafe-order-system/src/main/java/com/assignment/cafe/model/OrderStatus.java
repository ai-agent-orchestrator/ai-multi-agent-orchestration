package com.assignment.cafe.model;

/*
 * 요구사항 5번(chap15 enum): 정해진 값 중 하나를 고르는 항목입니다.
 *
 * 주문 상태는 아무 문자열이나 들어가면 안 됩니다.
 * "접수 대기", "제조 중", "준비 완료", "수령 완료", "취소"처럼 정해진 상태 중 하나여야 합니다.
 * enum을 사용하면 오타를 줄이고, 잘못된 상태값이 들어오는 것을 막을 수 있습니다.
 */
public enum OrderStatus {
    WAITING("접수 대기"),
    MAKING("제조 중"),
    READY("준비 완료"),
    COMPLETED("수령 완료"),
    CANCELED("취소");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromMenuNumber(int number) {
        /*
         * 사용자는 enum 이름 대신 1, 2, 3 같은 번호를 입력합니다.
         * 번호가 범위를 벗어나면 예외를 발생시켜 잘못된 입력임을 알려줍니다.
         */
        if (number < 1 || number > values().length) {
            throw new IllegalArgumentException("존재하지 않는 주문 상태입니다.");
        }

        return values()[number - 1];
    }
}
