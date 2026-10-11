package com.ohgiraffers.restaurant.payment.domain;

import com.ohgiraffers.restaurant.order.domain.Order;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/* 설명. 결제 엔티티
 *  이 프로젝트는 실제 결제 대행사(PG) API를 호출하지 않는다.
 *  결제 수단('CARD' 또는 'CASH')과 금액을 기록하는 것으로 결제를 흉내낸다.
 * */
@Entity
@Table(name = "tbl_payment")
public class Payment {

    @Id
    @Column(name = "payment_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int paymentCode;

    /* 설명. 주문 1건에 결제 1건 (1:1). 외래키(order_code)를 가진 이쪽이 연관 관계의 주인이다. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_code")
    private Order order;

    @Column(name = "payment_datetime")
    private LocalDateTime paymentDatetime;

    @Column(name = "payment_price")
    private int paymentPrice;

    @Column(name = "payment_type")
    private String paymentType;

    public Payment() {}

    public Payment(Order order, int paymentPrice, String paymentType) {
        this.order = order;
        this.paymentDatetime = LocalDateTime.now();
        this.paymentPrice = paymentPrice;
        this.paymentType = paymentType;
    }

    public int getPaymentCode() {
        return paymentCode;
    }

    public Order getOrder() {
        return order;
    }

    public LocalDateTime getPaymentDatetime() {
        return paymentDatetime;
    }

    public int getPaymentPrice() {
        return paymentPrice;
    }

    public String getPaymentType() {
        return paymentType;
    }
}
