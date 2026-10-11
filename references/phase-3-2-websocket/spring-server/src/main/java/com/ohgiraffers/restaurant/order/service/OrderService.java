package com.ohgiraffers.restaurant.order.service;

import com.ohgiraffers.restaurant.cart.service.CartService;
import com.ohgiraffers.restaurant.common.exception.NotFoundException;
import com.ohgiraffers.restaurant.member.domain.Member;
import com.ohgiraffers.restaurant.member.domain.MemberRepository;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import com.ohgiraffers.restaurant.menu.domain.MenuRepository;
import com.ohgiraffers.restaurant.order.domain.Order;
import com.ohgiraffers.restaurant.order.domain.OrderRepository;
import com.ohgiraffers.restaurant.order.dto.OrderItemRequest;
import com.ohgiraffers.restaurant.order.dto.OrderRequest;
import com.ohgiraffers.restaurant.order.dto.OrderResponse;
import com.ohgiraffers.restaurant.payment.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final MenuRepository menuRepository;
    private final PaymentService paymentService;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        MemberRepository memberRepository,
                        MenuRepository menuRepository,
                        PaymentService paymentService,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.menuRepository = menuRepository;
        this.paymentService = paymentService;
        this.cartService = cartService;
    }

    /* 목차. 1. 주문하기 (주문 + 주문별 메뉴 + 결제) */
    /**
     * 주문을 만들고 곧바로 결제까지 처리한다.
     *
     * <p>세 테이블(tbl_order, tbl_order_menu, tbl_payment)에 저장하고 장바구니(tbl_cart)를 비우는 작업이
     * 하나의 트랜잭션으로 묶인다.
     * 중간에 하나라도 실패하면(예: 주문할 수 없는 메뉴가 섞여 있음) 전부 롤백되어,
     * 결제 없는 주문이나 메뉴 없는 주문이 남지 않는다.</p>
     *
     * <p>[Phase 2] 주문한 회원(memberCode)은 요청 본문이 아니라 컨트롤러가 토큰에서 꺼내 넘겨준 값이다.</p>
     *
     * @param memberCode 주문한 회원 코드 (토큰에서 꺼낸 값)
     * @param orderRequest 주문 요청 (결제 수단, 메뉴 목록)
     * @return 완료된 주문 정보
     * @throws NotFoundException 회원 또는 메뉴가 존재하지 않는 경우
     * @throws IllegalArgumentException 주문할 수 없는 메뉴가 포함된 경우
     */
    @Transactional
    public OrderResponse placeOrder(int memberCode, OrderRequest orderRequest) {

        Member member = memberRepository.findById(memberCode)
                .orElseThrow(() -> new NotFoundException("해당 회원이 존재하지 않습니다. memberCode=" + memberCode));

        Order order = new Order(member);

        /* 설명. 장바구니에 같은 메뉴가 두 줄로 담겨 오면 수량을 합친다.
         *  tbl_order_menu에는 (주문코드, 메뉴코드) UNIQUE 제약 조건이 있어서 같은 메뉴가 두 행으로 들어갈 수 없다.
         *  LinkedHashMap을 쓴 이유는 손님이 담은 순서를 그대로 유지하기 위해서다.
         * */
        Map<Integer, Integer> amountByMenuCode = orderRequest.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::getMenuCode,
                        OrderItemRequest::getOrderAmount,
                        Integer::sum,
                        LinkedHashMap::new
                ));

        for (Map.Entry<Integer, Integer> entry : amountByMenuCode.entrySet()) {

            Menu menu = menuRepository.findById(entry.getKey())
                    .filter(foundMenu -> !foundMenu.isDeleted())
                    .orElseThrow(() -> new NotFoundException("해당 메뉴가 존재하지 않습니다. menuCode=" + entry.getKey()));

            if (!menu.isOrderable()) {
                throw new IllegalArgumentException("현재 주문할 수 없는 메뉴입니다: " + menu.getMenuName());
            }

            order.addOrderMenu(menu, entry.getValue());
        }

        // cascade 설정 덕분에 주문을 저장하면 주문별 메뉴도 함께 INSERT 된다.
        orderRepository.save(order);

        // 결제는 결제 도메인에 맡긴다. (같은 트랜잭션 안에서 실행된다)
        paymentService.pay(order, orderRequest.getPaymentType());

        // 주문을 마쳤으므로 장바구니를 비운다. (역시 같은 트랜잭션 안에서 실행된다)
        cartService.clearCart(member.getMemberCode());

        return OrderResponse.from(order);
    }

    /* 목차. 2. 회원별 주문 내역 조회 */
    @Transactional(readOnly = true)
    public List<OrderResponse> findOrdersByMember(int memberCode) {

        if (!memberRepository.existsById(memberCode)) {
            throw new NotFoundException("해당 회원이 존재하지 않습니다. memberCode=" + memberCode);
        }

        return orderRepository.findOrdersWithDetailsByMemberCode(memberCode).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /* 목차. 3. 전체 주문 내역 조회 (관리자) */
    @Transactional(readOnly = true)
    public List<OrderResponse> findAllOrders() {

        return orderRepository.findAllOrdersWithDetails().stream()
                .map(OrderResponse::from)
                .toList();
    }
}
