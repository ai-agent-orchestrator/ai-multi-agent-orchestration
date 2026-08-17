package com.assignment.cafe.controller;

import com.assignment.cafe.exception.OrderNotFoundException;
import com.assignment.cafe.model.CafeOrder;
import com.assignment.cafe.model.DiscountPolicy;
import com.assignment.cafe.model.OrderItem;
import com.assignment.cafe.model.OrderStatus;
import com.assignment.cafe.service.OrderService;
import com.assignment.cafe.view.OutputView;

import java.util.List;

/*
 * 화면(View)과 주문 처리(Service) 사이를 연결하는 Controller입니다.
 * 사용자가 요청한 작업을 Service에 전달하고, 결과를 View에 출력합니다.
 */
public class CafeOrderController {
    private final OrderService orderService;
    private final OutputView outputView;

    public CafeOrderController(OrderService orderService, OutputView outputView) {
        this.orderService = orderService;
        this.outputView = outputView;
    }

    public CafeOrder registerOrder(String customerName, List<OrderItem> items, DiscountPolicy discountPolicy) {
        try {
            CafeOrder order = orderService.createOrder(customerName, items, discountPolicy);
            outputView.printMessage("[완료] 주문이 등록되었습니다.");
            outputView.printOrder(order);
            outputView.printMessage("[매출 반영] 현재 오늘 매출 합계: "
                    + String.format("%,d원", orderService.calculateTodaySales()));
            return order;
        } catch (IllegalArgumentException e) {
            outputView.printError(e.getMessage());
            return null;
        }
    }

    public void showAllOrders() {
        outputView.printOrders(orderService.findAll());
    }

    public void showOrderById(int id) {
        try {
            outputView.printOrder(orderService.findById(id));
        } catch (OrderNotFoundException e) {
            outputView.printError(e.getMessage());
        }
    }

    public void searchByCustomerName(String keyword) {
        outputView.printMessage("'" + keyword + "' 고객명 검색 결과입니다.");
        outputView.printOrders(orderService.searchByCustomerName(keyword));
    }

    public void searchByMenuName(String keyword) {
        outputView.printMessage("'" + keyword + "' 메뉴명 검색 결과입니다.");
        outputView.printOrders(orderService.searchByMenuName(keyword));
    }

    public void filterByStatus(OrderStatus status) {
        outputView.printMessage(status.getDescription() + " 상태의 주문입니다.");
        outputView.printOrders(orderService.filterByStatus(status));
    }

    public void updateCustomerName(int id, String customerName) {
        try {
            orderService.updateCustomerName(id, customerName);
            outputView.printMessage("[완료] 고객명이 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            outputView.printError(e.getMessage());
        }
    }

    public void updateOrderStatus(int id, OrderStatus status) {
        try {
            orderService.updateOrderStatus(id, status);
            outputView.printMessage("[완료] 주문 상태가 수정되었습니다.");
        } catch (OrderNotFoundException e) {
            outputView.printError(e.getMessage());
        }
    }

    public void deleteOrder(int id) {
        try {
            orderService.deleteOrder(id);
            outputView.printMessage("[완료] 주문이 삭제되었습니다.");
        } catch (OrderNotFoundException e) {
            outputView.printError(e.getMessage());
        }
    }

    public void showTodaySales() {
        List<CafeOrder> salesOrders = orderService.findSalesOrders();
        outputView.printSalesSummary(salesOrders, orderService.calculateTodaySales());
    }
}
