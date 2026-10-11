package com.ohgiraffers.restaurant.order.domain;

import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import com.ohgiraffers.restaurant.payment.domain.Payment;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_order")
public class Order {

    @Id
    @Column(name = "order_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_code")
    private Member member;

    /* 설명. DATETIME 컬럼은 Java의 LocalDateTime으로 매핑된다. (날짜와 시간을 함께 가진다) */
    @Column(name = "order_datetime")
    private LocalDateTime orderDatetime;

    @Column(name = "total_order_price")
    private int totalOrderPrice;

    /* 설명. 주문에 담긴 메뉴 목록 (1:N)
     *  cascade = ALL 이므로 주문을 저장하면 이 목록에 담긴 OrderMenu도 함께 INSERT 된다.
     * */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderMenu> orderMenus = new ArrayList<>();

    /* 설명. 결제 정보 (1:1)
     *  외래키(order_code)는 tbl_payment 쪽에 있으므로 Payment가 연관 관계의 주인이고,
     *  이쪽은 mappedBy로 읽기만 한다.
     * */
    @OneToOne(mappedBy = "order")
    private Payment payment;

    public Order() {}

    public Order(Member member) {
        this.member = member;
        this.orderDatetime = LocalDateTime.now();
        this.totalOrderPrice = 0;
    }

    /* 목차. 주문에 메뉴 담기 */
    /* 설명. 총 주문 금액은 서버가 DB에 저장된 메뉴 가격으로 계산한다.
     *  클라이언트가 보낸 가격을 그대로 믿으면, 요청을 조작해 가격을 1원으로 바꿔 보낼 수 있기 때문이다.
     * */
    public void addOrderMenu(Menu menu, int orderAmount) {
        this.orderMenus.add(new OrderMenu(this, menu, orderAmount));
        this.totalOrderPrice += menu.getMenuPrice() * orderAmount;
    }

    /* 목차. 결제 완료 처리 (결제 도메인이 결제를 만든 뒤 호출한다) */
    public void completePayment(Payment payment) {
        this.payment = payment;
    }

    public int getOrderCode() {
        return orderCode;
    }

    public Member getMember() {
        return member;
    }

    public LocalDateTime getOrderDatetime() {
        return orderDatetime;
    }

    public int getTotalOrderPrice() {
        return totalOrderPrice;
    }

    public List<OrderMenu> getOrderMenus() {
        return orderMenus;
    }

    public Payment getPayment() {
        return payment;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderCode=" + orderCode +
                ", orderDatetime=" + orderDatetime +
                ", totalOrderPrice=" + totalOrderPrice +
                '}';
    }
}
