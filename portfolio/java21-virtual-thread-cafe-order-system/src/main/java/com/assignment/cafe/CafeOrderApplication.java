package com.assignment.cafe;

import com.assignment.cafe.controller.CafeOrderController;
import com.assignment.cafe.model.CafeOrder;
import com.assignment.cafe.model.DiscountPolicy;
import com.assignment.cafe.model.NoDiscountPolicy;
import com.assignment.cafe.model.OrderItem;
import com.assignment.cafe.model.OrderStatus;
import com.assignment.cafe.model.TakeOutDiscountPolicy;
import com.assignment.cafe.repository.OrderRepository;
import com.assignment.cafe.service.MenuService;
import com.assignment.cafe.service.OrderService;
import com.assignment.cafe.service.VirtualThreadOrderProcessor;
import com.assignment.cafe.view.InputView;
import com.assignment.cafe.view.OutputView;

import java.util.ArrayList;
import java.util.List;

/*
 * 프로그램의 시작점입니다.
 *
 * 이 클래스는 "전체 메뉴 흐름"만 담당합니다.
 * 실제 주문 데이터 처리 로직은 OrderService, 화면 출력은 OutputView, 입력은 InputView가 담당합니다.
 *
 * 요구사항 1번(chap04 제어문)
 * - run() 메소드의 while 문으로 콘솔 메뉴가 계속 반복됩니다.
 * - 사용자가 0번을 고르면 running 값이 false가 되어 반복문이 끝나고 정상 종료됩니다.
 *
 * 요구사항 7번(chap06, chap09 역할 분리)
 * - Application은 메뉴 진행만 합니다.
 * - Controller, Service, Repository, View, Model을 따로 두어 관심사를 분리했습니다.
 */
public class CafeOrderApplication {
    /*
     * 필요한 객체를 한 번씩 만들어 연결합니다.
     *
     * 초보자가 흔히 하는 실수는 메뉴를 실행할 때마다 Repository를 새로 만드는 것입니다.
     * 그러면 이전에 등록한 주문 데이터가 사라집니다.
     * 그래서 프로그램이 실행되는 동안 같은 Repository 객체를 계속 사용합니다.
     */
    private final InputView inputView = new InputView();
    private final OutputView outputView = new OutputView();
    private final MenuService menuService = new MenuService();
    private final OrderRepository orderRepository = new OrderRepository();
    private final OrderService orderService = new OrderService(orderRepository);
    private final CafeOrderController cafeOrderController = new CafeOrderController(orderService, outputView);
    private final VirtualThreadOrderProcessor virtualThreadOrderProcessor = new VirtualThreadOrderProcessor(orderService);

    public static void main(String[] args) {
        new CafeOrderApplication().run();
    }

    public void run() {
        boolean running = true;

        try {
            /*
             * 요구사항 1번: 반복 메뉴
             *
             * while (running)은 running이 true인 동안 계속 반복됩니다.
             * 사용자가 잘못된 메뉴 번호를 골라도 프로그램은 종료되지 않고,
             * 오류 메시지를 보여준 뒤 다시 메뉴를 출력합니다.
             */
            while (running) {
                outputView.printMainMenu();
                int menu = inputView.readInt("메뉴 선택: ");

                try {
                    /*
                     * Java 14 이후부터 사용할 수 있는 switch 화살표 문법입니다.
                     * break를 쓰지 않아도 다음 case로 흘러가지 않아서 실수를 줄일 수 있습니다.
                     */
                    switch (menu) {
                        case 1 -> createOrder();
                        case 2 -> cafeOrderController.showAllOrders();
                        case 3 -> findOrderById();
                        case 4 -> searchByCustomerName();
                        case 5 -> searchByMenuName();
                        case 6 -> filterByStatus();
                        case 7 -> updateOrder();
                        case 8 -> deleteOrder();
                        case 9 -> printSales();
                        case 0 -> running = false;
                        default -> outputView.printError("메뉴 번호는 0부터 9까지 입력해주세요.");
                    }
                } catch (IllegalArgumentException e) {
                    /*
                     * 요구사항 8번: 예외 처리
                     *
                     * 예를 들어 존재하지 않는 메뉴 번호나 상태 번호가 들어오면
                     * IllegalArgumentException이 발생할 수 있습니다.
                     * 여기서 잡아주기 때문에 프로그램이 꺼지지 않고 다시 메뉴로 돌아갑니다.
                    */
                    outputView.printError(e.getMessage());
                }

                if (running) {
                    inputView.waitForEnter("계속하려면 Enter를 누르세요...");
                }
            }
        } finally {
            virtualThreadOrderProcessor.close();
        }

        outputView.printMessage("프로그램을 정상 종료합니다.");
    }

    private void createOrder() {
        /*
         * 요구사항 4번: Create(등록)
         *
         * 고객명, 주문 메뉴, 할인 정책을 입력받아 Controller에 넘깁니다.
         * 실제 저장은 Service와 Repository가 담당합니다.
         */
        String customerName = inputView.readLine("고객명: ");
        List<OrderItem> items = readOrderItems();
        DiscountPolicy discountPolicy = readDiscountPolicy();

        int originalPrice = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();
        int finalPrice = originalPrice - discountPolicy.discount(originalPrice);

        outputView.printMessage("결제 예정 금액: " + String.format("%,d원", finalPrice));

        if (!inputView.readYesNo("결제하시겠습니까?")) {
            outputView.printMessage("결제가 취소되어 주문을 등록하지 않습니다.");
            return;
        }

        outputView.printMessage("결제가 완료되었습니다. 주문을 저장하고 가상 스레드 제조 작업을 준비합니다.");
        CafeOrder order = cafeOrderController.registerOrder(customerName, items, discountPolicy);

        if (order != null) {
            virtualThreadOrderProcessor.startProcessing(order.getId());
        }
    }

    private List<OrderItem> readOrderItems() {
        /*
         * 요구사항 3번: 컬렉션
         *
         * 한 주문에는 아메리카노 2잔, 치즈케이크 1개처럼 여러 항목이 들어갈 수 있습니다.
         * 그래서 OrderItem 여러 개를 List<OrderItem>에 담아 관리합니다.
         */
        List<OrderItem> items = new ArrayList<>();
        boolean adding = true;

        while (adding) {
            outputView.printMenus(menuService.findAll());
            int menuId = inputView.readInt("주문할 메뉴 번호: ");
            int quantity = inputView.readInt("수량: ");

            items.add(new OrderItem(menuService.findById(menuId), quantity));
            adding = inputView.readYesNo("메뉴를 더 추가하시겠습니까?");
        }

        return items;
    }

    private DiscountPolicy readDiscountPolicy() {
        boolean takeOut = inputView.readYesNo("포장 주문입니까?");

        /*
         * 요구사항 7번의 다형성 활용
         *
         * DiscountPolicy는 인터페이스입니다.
         * 포장 주문이면 TakeOutDiscountPolicy, 매장 주문이면 NoDiscountPolicy를 반환합니다.
         *
         * Application 입장에서는 "어떤 할인 정책인지"를 자세히 몰라도
         * DiscountPolicy 타입으로 똑같이 사용할 수 있습니다.
         */
        if (takeOut) {
            return new TakeOutDiscountPolicy();
        }

        return new NoDiscountPolicy();
    }

    private void findOrderById() {
        // 요구사항 4번: Read(주문번호로 조회)
        int id = inputView.readInt("조회할 주문번호: ");
        cafeOrderController.showOrderById(id);
    }

    private void searchByCustomerName() {
        // 요구사항 6번: Stream API 검색 기능 1 - 고객명 키워드 검색
        String keyword = inputView.readLine("검색할 고객명 키워드: ");
        cafeOrderController.searchByCustomerName(keyword);
    }

    private void searchByMenuName() {
        // 요구사항 6번: Stream API 검색 기능 2 - 메뉴명 키워드 검색
        String keyword = inputView.readLine("검색할 메뉴명 키워드: ");
        cafeOrderController.searchByMenuName(keyword);
    }

    private void filterByStatus() {
        // 요구사항 6번: Stream API 필터 기능 3 - enum 상태별 필터
        OrderStatus status = readOrderStatus();
        cafeOrderController.filterByStatus(status);
    }

    private void updateOrder() {
        // 요구사항 4번: Update(수정) - 고객명 또는 주문 상태를 수정합니다.
        outputView.printUpdateMenu();
        int updateMenu = inputView.readInt("수정 메뉴 선택: ");
        int id = inputView.readInt("수정할 주문번호: ");

        switch (updateMenu) {
            case 1 -> {
                String customerName = inputView.readLine("새 고객명: ");
                cafeOrderController.updateCustomerName(id, customerName);
            }
            case 2 -> {
                OrderStatus status = readOrderStatus();
                cafeOrderController.updateOrderStatus(id, status);
            }
            default -> outputView.printError("수정 메뉴는 1 또는 2만 선택할 수 있습니다.");
        }
    }

    private OrderStatus readOrderStatus() {
        /*
         * 요구사항 5번: enum
         *
         * 주문 상태는 WAITING, MAKING, READY, COMPLETED, CANCELED 중 하나만 가능합니다.
         * 이렇게 정해진 선택지를 enum으로 만들면 오타나 잘못된 상태값을 줄일 수 있습니다.
         */
        outputView.printOrderStatuses();
        int statusNumber = inputView.readInt("상태 번호 선택: ");
        return OrderStatus.fromMenuNumber(statusNumber);
    }

    private void deleteOrder() {
        // 요구사항 4번: Delete(삭제)
        int id = inputView.readInt("삭제할 주문번호: ");
        boolean confirmed = inputView.readYesNo("정말 삭제하시겠습니까?");

        if (!confirmed) {
            outputView.printMessage("삭제를 취소했습니다.");
            return;
        }

        cafeOrderController.deleteOrder(id);
    }

    private void printSales() {
        cafeOrderController.showTodaySales();
    }
}
