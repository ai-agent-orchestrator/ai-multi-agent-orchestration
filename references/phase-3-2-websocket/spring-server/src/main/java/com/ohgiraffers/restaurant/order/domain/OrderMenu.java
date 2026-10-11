package com.ohgiraffers.restaurant.order.domain;

import com.ohgiraffers.restaurant.menu.domain.Menu;
import jakarta.persistence.*;

/* 설명. 주문과 메뉴의 다대다(N:M) 관계를 풀어낸 연관 엔티티 (주문별 메뉴)
 *  menudb에서는 (주문코드, 메뉴코드) 복합키였지만, 이 프로젝트에서는 인조식별자 하나를 @Id로 둔다.
 * */
@Entity
@Table(name = "tbl_order_menu")
public class OrderMenu {

    @Id
    @Column(name = "order_menu_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderMenuCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_code")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_code")
    private Menu menu;

    @Column(name = "order_amount")
    private int orderAmount;

    public OrderMenu() {}

    public OrderMenu(Order order, Menu menu, int orderAmount) {
        this.order = order;
        this.menu = menu;
        this.orderAmount = orderAmount;
    }

    public int getOrderMenuCode() {
        return orderMenuCode;
    }

    public Order getOrder() {
        return order;
    }

    public Menu getMenu() {
        return menu;
    }

    public int getOrderAmount() {
        return orderAmount;
    }
}
