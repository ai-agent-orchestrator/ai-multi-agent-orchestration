package com.ohgiraffers.restaurant.order.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.order.dto.OrderResponse;
import com.ohgiraffers.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "주문 관리 (관리자)", description = "전체 주문 내역")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /* 목차. 1. 전체 주문 내역 조회 */
    @Operation(summary = "전체 주문 내역 (관리자용)", description = """
            모든 회원의 주문을 최근 주문부터 조회한다. 페이징은 없다.
            - 200 : result.orders (배열. 주문하기 응답의 order와 같은 모양)
            - 401 : 토큰 없음·만료 / 403 : 관리자가 아님
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findAllOrders() {

        List<OrderResponse> orders = orderService.findAllOrders();

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "전체 주문 내역 조회 성공", Map.of("orders", orders)));
    }
}
