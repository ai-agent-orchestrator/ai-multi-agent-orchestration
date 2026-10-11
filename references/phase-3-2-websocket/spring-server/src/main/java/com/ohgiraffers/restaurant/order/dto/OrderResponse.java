package com.ohgiraffers.restaurant.order.dto;

import com.ohgiraffers.restaurant.order.domain.Order;

import java.time.LocalDateTime;
import java.util.List;

/* 설명. 주문 응답 DTO (주문 + 결제 + 주문한 메뉴 목록)
 *  orderDatetime(LocalDateTime)은 JSON에서 "2026-09-18T12:30:00" 형태의 문자열로 바뀐다.
 * */
public class OrderResponse {

    private int orderCode;
    private int memberCode;
    private String memberName;
    private LocalDateTime orderDatetime;
    private int totalOrderPrice;
    private String paymentType;
    private List<OrderMenuResponse> orderMenus;

    public OrderResponse() {}

    public static OrderResponse from(Order order) {

        OrderResponse response = new OrderResponse();
        response.orderCode = order.getOrderCode();
        response.memberCode = order.getMember().getMemberCode();
        response.memberName = order.getMember().getMemberName();
        response.orderDatetime = order.getOrderDatetime();
        response.totalOrderPrice = order.getTotalOrderPrice();
        response.paymentType = order.getPayment() != null ? order.getPayment().getPaymentType() : null;
        response.orderMenus = order.getOrderMenus().stream()
                .map(OrderMenuResponse::from)
                .toList();

        return response;
    }

    public int getOrderCode() {
        return orderCode;
    }

    public int getMemberCode() {
        return memberCode;
    }

    public String getMemberName() {
        return memberName;
    }

    public LocalDateTime getOrderDatetime() {
        return orderDatetime;
    }

    public int getTotalOrderPrice() {
        return totalOrderPrice;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public List<OrderMenuResponse> getOrderMenus() {
        return orderMenus;
    }
}
