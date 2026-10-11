package com.ohgiraffers.restaurant.cart.domain;

import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import jakarta.persistence.*;

/* 설명. 회원과 메뉴의 다대다(N:M) 관계를 풀어낸 연관 엔티티 (장바구니에 담긴 메뉴 한 줄)
 *  주문별 메뉴(OrderMenu)와 같은 모양이다. 인조식별자 하나를 @Id로 두고,
 *  같은 회원이 같은 메뉴를 두 줄로 담지 않도록 DB 에 (회원코드, 메뉴코드) UNIQUE 제약 조건을 걸어 두었다.
 * */
@Entity
@Table(name = "tbl_cart")
public class Cart {

    @Id
    @Column(name = "cart_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int cartCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_code")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_code")
    private Menu menu;

    @Column(name = "cart_amount")
    private int cartAmount;

    public Cart() {}

    public Cart(Member member, Menu menu, int cartAmount) {
        this.member = member;
        this.menu = menu;
        this.cartAmount = cartAmount;
    }

    // 이미 담긴 메뉴를 또 담으면 수량만 더한다.
    public void addAmount(int amount) {
        this.cartAmount += amount;
    }

    public void changeAmount(int cartAmount) {
        this.cartAmount = cartAmount;
    }

    public int getCartCode() {
        return cartCode;
    }

    public Member getMember() {
        return member;
    }

    public Menu getMenu() {
        return menu;
    }

    public int getCartAmount() {
        return cartAmount;
    }
}
