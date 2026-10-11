package com.ohgiraffers.restaurant.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    /* 설명. 주문 내역 화면에는 회원, 결제, 주문별 메뉴, 메뉴가 모두 필요하다.
     *  하나씩 지연 로딩으로 꺼내면 주문 수 × 메뉴 수만큼 SELECT가 늘어나므로 페치 조인으로 한 번에 가져온다.
     *  ----------------------------------------------------------------------------------
     *  참고. 컬렉션(orderMenus)을 페치 조인하면 주문 한 건이 메뉴 수만큼 행으로 늘어나지만,
     *       Hibernate 6부터는 중복된 주문 엔티티를 자동으로 하나로 합쳐준다. (DISTINCT를 쓰지 않아도 된다)
     *  주의. 컬렉션을 페치 조인한 쿼리에는 페이징을 걸지 않는다.
     *       DB가 아니라 메모리에서 페이징하게 되어 데이터가 많으면 성능 문제가 생기기 때문이다.
     * */

    /* 목차. 1. 특정 회원의 주문 내역 (최근 주문부터) */
    @Query("""
            SELECT o FROM Order o
              JOIN FETCH o.member m
              JOIN FETCH o.payment
              JOIN FETCH o.orderMenus om
              JOIN FETCH om.menu
             WHERE m.memberCode = :memberCode
             ORDER BY o.orderDatetime DESC
            """)
    List<Order> findOrdersWithDetailsByMemberCode(@Param("memberCode") int memberCode);

    /* 목차. 2. 전체 주문 내역 (관리자용, 최근 주문부터) */
    @Query("""
            SELECT o FROM Order o
              JOIN FETCH o.member
              JOIN FETCH o.payment
              JOIN FETCH o.orderMenus om
              JOIN FETCH om.menu
             ORDER BY o.orderDatetime DESC
            """)
    List<Order> findAllOrdersWithDetails();
}
