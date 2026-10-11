package com.ohgiraffers.restaurant.cart.service;

import com.ohgiraffers.restaurant.cart.domain.Cart;
import com.ohgiraffers.restaurant.cart.domain.CartRepository;
import com.ohgiraffers.restaurant.cart.dto.CartItemRequest;
import com.ohgiraffers.restaurant.cart.dto.CartItemResponse;
import com.ohgiraffers.restaurant.common.exception.NotFoundException;
import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.member.domain.MemberRepository;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import com.ohgiraffers.restaurant.menu.domain.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/* 설명. 담기 · 수량 변경 · 빼기는 모두 '바뀐 뒤의 장바구니 전체'를 돌려준다.
 *  React 는 받은 목록으로 화면과 헤더의 개수 배지를 한 번에 다시 그리면 된다.
 * */
@Service
public class CartService {

    private static final int MAX_AMOUNT = 99;

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final MenuRepository menuRepository;

    public CartService(CartRepository cartRepository,
                       MemberRepository memberRepository,
                       MenuRepository menuRepository) {
        this.cartRepository = cartRepository;
        this.memberRepository = memberRepository;
        this.menuRepository = menuRepository;
    }

    /* 목차. 1. 장바구니 조회 */
    @Transactional(readOnly = true)
    public List<CartItemResponse> findCart(int memberCode) {

        if (!memberRepository.existsById(memberCode)) {
            throw new NotFoundException("해당 회원이 존재하지 않습니다. memberCode=" + memberCode);
        }

        return toResponses(memberCode);
    }

    /* 목차. 2. 장바구니 담기 (이미 담긴 메뉴면 수량을 더한다) */
    @Transactional
    public List<CartItemResponse> addCartItem(int memberCode, CartItemRequest cartItemRequest) {

        Member member = memberRepository.findById(memberCode)
                .orElseThrow(() -> new NotFoundException("해당 회원이 존재하지 않습니다. memberCode=" + memberCode));

        Menu menu = menuRepository.findById(cartItemRequest.getMenuCode())
                .filter(foundMenu -> !foundMenu.isDeleted())
                .orElseThrow(() -> new NotFoundException("해당 메뉴가 존재하지 않습니다. menuCode=" + cartItemRequest.getMenuCode()));

        if (!menu.isOrderable()) {
            throw new IllegalArgumentException("현재 주문할 수 없는 메뉴입니다: " + menu.getMenuName());
        }

        int amount = cartItemRequest.getCartAmount();

        cartRepository.findByMember_MemberCodeAndMenu_MenuCode(memberCode, menu.getMenuCode())
                .ifPresentOrElse(
                        cart -> {
                            if (cart.getCartAmount() + amount > MAX_AMOUNT) {
                                throw new IllegalArgumentException("한 메뉴는 " + MAX_AMOUNT + "개까지 담을 수 있습니다.");
                            }
                            cart.addAmount(amount);    // 변경 감지로 UPDATE 된다
                        },
                        () -> cartRepository.save(new Cart(member, menu, amount))
                );

        return toResponses(memberCode);
    }

    /* 목차. 3. 수량 변경 */
    @Transactional
    public List<CartItemResponse> changeCartAmount(int memberCode, int menuCode, int cartAmount) {

        Cart cart = findCartItem(memberCode, menuCode);
        cart.changeAmount(cartAmount);

        return toResponses(memberCode);
    }

    /* 목차. 4. 장바구니에서 빼기 */
    @Transactional
    public List<CartItemResponse> removeCartItem(int memberCode, int menuCode) {

        cartRepository.delete(findCartItem(memberCode, menuCode));

        return toResponses(memberCode);
    }

    /* 목차. 5. 장바구니 비우기 (주문을 마쳤을 때 OrderService 가 부른다) */
    @Transactional
    public void clearCart(int memberCode) {
        cartRepository.deleteAllByMemberCode(memberCode);
    }

    private Cart findCartItem(int memberCode, int menuCode) {
        return cartRepository.findByMember_MemberCodeAndMenu_MenuCode(memberCode, menuCode)
                .orElseThrow(() -> new NotFoundException("장바구니에 없는 메뉴입니다. menuCode=" + menuCode));
    }

    private List<CartItemResponse> toResponses(int memberCode) {
        return cartRepository.findCartsWithMenuByMemberCode(memberCode).stream()
                .map(CartItemResponse::from)
                .toList();
    }
}
