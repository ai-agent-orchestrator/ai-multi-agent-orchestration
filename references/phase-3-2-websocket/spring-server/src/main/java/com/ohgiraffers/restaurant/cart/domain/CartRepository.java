package com.ohgiraffers.restaurant.cart.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    /* 설명. 회원의 장바구니를 메뉴와 함께 한 번에 가져온다. (JOIN FETCH, N+1 방지)
     *  담아 둔 사이에 관리자가 삭제한 메뉴는 보여주지 않는다. 담은 순서대로 정렬한다.
     * */
    @Query("""
            SELECT c FROM Cart c
              JOIN FETCH c.menu m
             WHERE c.member.memberCode = :memberCode
               AND m.deletedStatus = 'N'
             ORDER BY c.cartCode
            """)
    List<Cart> findCartsWithMenuByMemberCode(@Param("memberCode") int memberCode);

    // 이 회원이 이 메뉴를 이미 담았는지 찾는다. (메서드 이름으로 만드는 쿼리)
    Optional<Cart> findByMember_MemberCodeAndMenu_MenuCode(int memberCode, int menuCode);

    /* 설명. 주문을 마친 회원의 장바구니를 한 번에 비운다.
     *  엔티티를 하나씩 조회해 지우지 않고 DELETE 문 한 번으로 지운다. (@Modifying)
     * */
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.member.memberCode = :memberCode")
    void deleteAllByMemberCode(@Param("memberCode") int memberCode);
}
