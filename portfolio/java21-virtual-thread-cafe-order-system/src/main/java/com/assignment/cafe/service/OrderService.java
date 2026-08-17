package com.assignment.cafe.service;

import com.assignment.cafe.exception.OrderNotFoundException;
import com.assignment.cafe.model.CafeOrder;
import com.assignment.cafe.model.DiscountPolicy;
import com.assignment.cafe.model.OrderItem;
import com.assignment.cafe.model.OrderStatus;
import com.assignment.cafe.repository.OrderRepository;

import java.util.List;

/*
 * 주문과 관련된 핵심 비즈니스 로직을 처리하는 클래스입니다.
 *
 * Repository는 데이터를 저장하고 찾는 일만 담당하고,
 * Service는 "주문 항목이 비어 있으면 안 된다", "없는 주문번호면 예외를 발생시킨다" 같은
 * 업무 규칙을 담당합니다.
 *
 * 요구사항 7번: 화면 출력과 데이터 처리를 분리하기 위해 View가 아니라 Service에 로직을 모았습니다.
 */
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public CafeOrder createOrder(String customerName, List<OrderItem> items, DiscountPolicy discountPolicy) {
        /*
         * 요구사항 4번: Create(등록)
         * 새 CafeOrder 객체를 만들고 Repository에 저장합니다.
         */
        validateCustomerName(customerName);

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 1개 이상 필요합니다.");
        }

        CafeOrder order = new CafeOrder(orderRepository.nextOrderId(), customerName.trim(), items, discountPolicy);
        return orderRepository.save(order);
    }

    public List<CafeOrder> findAll() {
        // 요구사항 4번: Read(전체 조회)
        return orderRepository.findAll();
    }

    public CafeOrder findById(int id) {
        /*
         * 요구사항 4번: Read(주문번호 조회)
         * 없는 주문번호를 입력하면 OrderNotFoundException을 던집니다.
         * 이 예외는 Controller에서 잡아 사용자에게 오류 메시지로 보여줍니다.
         */
        CafeOrder order = orderRepository.findById(id);

        if (order == null) {
            throw new OrderNotFoundException(id);
        }

        return order;
    }

    public List<CafeOrder> searchByCustomerName(String keyword) {
        /*
         * 요구사항 6번: Stream API 검색 기능 1
         * 고객명에 keyword가 포함된 주문만 filter로 골라냅니다.
         */
        return orderRepository.findAll().stream()
                .filter(order -> order.getCustomerName().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    public List<CafeOrder> searchByMenuName(String keyword) {
        /*
         * 요구사항 6번: Stream API 검색 기능 2
         * 주문 항목 중 메뉴명에 keyword가 포함된 주문만 골라냅니다.
         */
        return orderRepository.findAll().stream()
                .filter(order -> order.containsMenuName(keyword))
                .toList();
    }

    public List<CafeOrder> filterByStatus(OrderStatus status) {
        /*
         * 요구사항 6번: Stream API 필터 기능 3
         * enum 상태값이 일치하는 주문만 filter로 골라냅니다.
         */
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    public void updateOrderStatus(int id, OrderStatus status) {
        // 요구사항 4번: Update(주문 상태 수정)
        CafeOrder order = findById(id);
        order.changeStatus(status);
    }

    public void updateCustomerName(int id, String customerName) {
        // 요구사항 4번: Update(고객명 수정)
        validateCustomerName(customerName);
        CafeOrder order = findById(id);
        order.changeCustomerName(customerName);
    }

    public void deleteOrder(int id) {
        // 요구사항 4번: Delete(삭제)
        boolean deleted = orderRepository.deleteById(id);

        if (!deleted) {
            throw new OrderNotFoundException(id);
        }
    }

    public int calculateTodaySales() {
        /*
         * Stream API 활용 예시입니다.
         * 취소되지 않은 주문만 filter로 남기고,
         * 각 주문의 최종 결제 금액을 mapToInt로 숫자 스트림으로 바꾼 뒤,
         * sum으로 합계를 구합니다.
         */
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELED)
                .mapToInt(CafeOrder::getFinalPrice)
                .sum();
    }

    public List<CafeOrder> findSalesOrders() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELED)
                .toList();
    }

    private void validateCustomerName(String customerName) {
        // 요구사항 8번: 잘못된 입력값을 저장하지 않도록 예외를 발생시킵니다.
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("고객명은 비워둘 수 없습니다.");
        }
    }
}
