package com.assignment.cafe.repository;

import com.assignment.cafe.model.CafeOrder;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * 주문 데이터를 보관하는 저장소 클래스입니다.
 * 아직 데이터베이스를 사용하지 않으므로 ArrayList를 DB처럼 사용합니다.
 * 화면 출력이나 메뉴 선택 판단은 하지 않고, 오직 저장/조회/삭제만 담당합니다.
 *
 * 요구사항 3번(chap12 컬렉션)
 * - 여러 건의 주문 데이터를 List<CafeOrder>에 담아 관리합니다.
 *
 * 요구사항 4번(chap12 CRUD)
 * - save(): 등록(Create)
 * - findAll(), findById(): 조회(Read)
 * - deleteById(): 삭제(Delete)
 * - 수정(Update)은 저장소에서 꺼낸 CafeOrder 객체의 값을 Service에서 바꾸는 방식으로 처리합니다.
 */
public class OrderRepository {
    /*
     * Java 21 가상 스레드 변형 버전에서는 메인 스레드가 주문을 조회하는 동안
     * 백그라운드 가상 스레드가 주문 상태를 바꿀 수 있습니다.
     *
     * CopyOnWriteArrayList는 조회가 많고 수정이 적은 작은 프로그램에서 사용하기 쉽습니다.
     * 조회 중 리스트 구조가 바뀌어도 ConcurrentModificationException이 발생하지 않습니다.
     */
    private final List<CafeOrder> orders = new CopyOnWriteArrayList<>();
    private int nextId = 1;

    public CafeOrder save(CafeOrder order) {
        // Create: 새 주문을 List에 추가합니다.
        orders.add(order);
        return order;
    }

    public int nextOrderId() {
        // 주문번호는 사용자가 직접 입력하지 않고 저장소가 자동으로 1씩 증가시켜 부여합니다.
        return nextId++;
    }

    public List<CafeOrder> findAll() {
        /*
         * Read: 전체 주문 조회입니다.
         * stream()과 sorted()를 사용해 주문번호 순서대로 정렬한 새 목록을 반환합니다.
         */
        return orders.stream()
                .sorted(Comparator.comparing(CafeOrder::getId))
                .toList();
    }

    public CafeOrder findById(int id) {
        /*
         * Read: 주문번호로 한 건을 찾습니다.
         * filter는 조건에 맞는 주문만 통과시키고, findFirst는 첫 번째 결과를 꺼냅니다.
         */
        return orders.stream()
                .filter(order -> order.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public boolean deleteById(int id) {
        /*
         * Delete: 주문번호가 일치하는 주문을 삭제합니다.
         * removeIf는 조건에 맞는 요소를 지우고, 실제로 지웠으면 true를 반환합니다.
         */
        return orders.removeIf(order -> order.getId() == id);
    }
}
