package com.ohgiraffers.restaurant.order.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import com.ohgiraffers.restaurant.order.dto.OrderRequest;
import com.ohgiraffers.restaurant.order.dto.OrderResponse;
import com.ohgiraffers.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "주문", description = "주문 · 결제 · 주문 내역")
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /* 목차. 1. 주문하기 */
    /* 설명. [Phase 2] 주문한 사람은 요청 본문이 아니라 토큰에서 정해진다.
     *  로그인한 사람만 들어오는 주소이므로 user 는 null 이 아니다.
     * */
    @Operation(summary = "주문하기", description = """
            주문과 결제를 한 번에 처리한다. 가격은 보내지 않는다. 서버가 DB의 메뉴 가격으로 계산한다.
            주문한 회원은 토큰에서 정해지므로 본문에 memberCode 를 넣지 않는다.
            본문: { paymentType('CARD'/'CASH'), items: [{ menuCode, orderAmount }] }
            - 201 : result.order (orderCode, memberCode, memberName, orderDatetime, totalOrderPrice, paymentType,
                    orderMenus[menuCode, menuName, menuPrice, orderAmount, menuImageUrl])
            - 400 : 입력값 검증 실패, 주문 불가 메뉴 포함 / 401 : 토큰 없음·만료 / 404 : 존재하지 않는 메뉴
            """)
    @PostMapping("/orders")
    public ResponseEntity<ResponseMessage> placeOrder(@AuthenticationPrincipal CustomUserDetails user,
                                                      @Valid @RequestBody OrderRequest orderRequest) {

        OrderResponse order = orderService.placeOrder(user.getMemberCode(), orderRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseMessage(HttpStatus.CREATED.value(), "주문 성공", Map.of("order", order)));
    }

    /* 목차. 2. 내 주문 내역 조회 */
    /* 설명. [Phase 2] /api/members/{memberCode}/orders 가 /api/members/me/orders 로 바뀌었다. */
    @Operation(summary = "내 주문 내역", description = """
            토큰 주인의 주문을 최근 주문부터 조회한다. 페이징은 없다.
            - 200 : result.orders (배열. 주문하기 응답의 order와 같은 모양)
            - 401 : 토큰 없음·만료
            """)
    @GetMapping("/members/me/orders")
    public ResponseEntity<ResponseMessage> findMyOrders(@AuthenticationPrincipal CustomUserDetails user) {

        List<OrderResponse> orders = orderService.findOrdersByMember(user.getMemberCode());

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "주문 내역 조회 성공", Map.of("orders", orders)));
    }
}
