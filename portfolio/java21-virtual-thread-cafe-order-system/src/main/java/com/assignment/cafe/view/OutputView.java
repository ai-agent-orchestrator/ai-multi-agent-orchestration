package com.assignment.cafe.view;

import com.assignment.cafe.model.CafeMenu;
import com.assignment.cafe.model.CafeOrder;
import com.assignment.cafe.model.OrderStatus;

import java.util.List;

/*
 * 화면 출력을 담당하는 View 클래스입니다.
 *
 * 요구사항 7번: 화면 출력과 데이터 처리를 분리했습니다.
 * System.out.println은 되도록 이 클래스에 모아두었습니다.
 * 이렇게 하면 나중에 콘솔 프로그램을 GUI나 웹 프로그램으로 바꿀 때 출력 부분만 교체하기 쉽습니다.
 */
public class OutputView {
    public void printMainMenu() {
        // 요구사항 1번: 반복해서 보여줄 메인 콘솔 메뉴입니다.
        System.out.println();
        System.out.println("========== 카페 주문 관리 프로그램 ==========");
        System.out.println("버전: Java 21 Virtual Thread + 결제/매출 반영 확인 버전");
        System.out.println("1. 주문 등록 및 결제");
        System.out.println("2. 전체 주문 조회");
        System.out.println("3. 주문번호로 조회");
        System.out.println("4. 고객명으로 검색");
        System.out.println("5. 메뉴명으로 검색");
        System.out.println("6. 주문 상태로 필터");
        System.out.println("7. 주문 수정");
        System.out.println("8. 주문 삭제");
        System.out.println("9. 오늘 매출 조회");
        System.out.println("0. 프로그램 종료");
        System.out.println("==========================================");
    }

    public void printMenus(List<CafeMenu> menus) {
        // 주문 등록 시 사용자가 고를 수 있는 카페 메뉴 목록을 출력합니다.
        System.out.println();
        System.out.println("------------- 메뉴판 -------------");
        menus.forEach(System.out::println);
        System.out.println("--------------------------------");
    }

    public void printUpdateMenu() {
        System.out.println("1. 고객명 수정");
        System.out.println("2. 주문 상태 수정");
    }

    public void printOrderStatuses() {
        /*
         * 요구사항 5번: enum 활용
         * OrderStatus.values()는 enum에 정의된 모든 상태값을 배열로 돌려줍니다.
         * 상태가 추가되어도 여기서 자동으로 출력됩니다.
         */
        for (OrderStatus status : OrderStatus.values()) {
            System.out.printf("%d. %s(%s)%n", status.ordinal() + 1, status.name(), status.getDescription());
        }
    }

    public void printOrders(List<CafeOrder> orders) {
        /*
         * 조회 결과가 없을 때 아무것도 출력하지 않으면 사용자는 프로그램이 멈춘 줄 알 수 있습니다.
         * 그래서 "조회된 주문이 없습니다."라고 명확히 알려줍니다.
         */
        if (orders.isEmpty()) {
            System.out.println("조회된 주문이 없습니다.");
            return;
        }

        orders.forEach(System.out::println);
    }

    public void printOrder(CafeOrder order) {
        System.out.println(order);
    }

    public void printSalesSummary(List<CafeOrder> orders, int totalSales) {
        System.out.println();
        System.out.println("---------- 오늘 매출 조회 ----------");
        System.out.println("매출 반영 주문 수: " + orders.size() + "건");

        if (orders.isEmpty()) {
            System.out.println("매출에 반영된 주문이 없습니다.");
        } else {
            orders.forEach(order -> System.out.printf(
                    "주문번호 %d | 고객명: %s | 상태: %s | 결제금액: %,d원%n",
                    order.getId(),
                    order.getCustomerName(),
                    order.getStatus().getDescription(),
                    order.getFinalPrice()
            ));
        }

        System.out.println("오늘 매출 합계: " + String.format("%,d원", totalSales));
        System.out.println("----------------------------------");
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }
}
