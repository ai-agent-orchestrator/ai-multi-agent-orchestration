package com.ohgiraffers.restaurant.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/* 설명. 주문 요청 본문(JSON)
 *  ----------------------------------------------------------------------------------
 *  {
 *    "paymentType": "CARD",
 *    "items": [
 *      { "menuCode": 1, "orderAmount": 2 },
 *      { "menuCode": 26, "orderAmount": 1 }
 *    ]
 *  }
 *  ----------------------------------------------------------------------------------
 *  [Phase 2] memberCode 필드가 사라졌다. 주문한 사람은 서버가 토큰에서 꺼낸다.
 *  (요청에 memberCode 를 끼워 보내도 받을 자리가 없어 무시된다)
 * */
public class OrderRequest {

    @NotBlank(message = "결제 수단은 필수입니다.")
    @Pattern(regexp = "CARD|CASH", message = "결제 수단은 CARD 또는 CASH여야 합니다.")
    private String paymentType;

    /* 설명. 목록 안의 각 항목(OrderItemRequest)까지 검증하려면 @Valid를 한 번 더 붙여야 한다. */
    @NotEmpty(message = "주문할 메뉴를 한 개 이상 담아주세요.")
    private List<@Valid OrderItemRequest> items;

    public OrderRequest() {}

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
