package com.assignment.cafe.service;

import com.assignment.cafe.exception.OrderNotFoundException;
import com.assignment.cafe.model.OrderStatus;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Java 21 가상 스레드를 사용하는 비동기 주문 제조 처리기입니다.
 *
 * 작은 콘솔 프로그램이라 실제 성능 이득은 크지 않지만,
 * 외부 API 호출, DB 대기, 결제 승인 대기처럼 "기다리는 작업"이 많은 서버에서
 * 가상 스레드를 어떻게 쓰는지 연습하기 위한 예시입니다.
 *
 * 핵심:
 * - Executors.newVirtualThreadPerTaskExecutor()는 작업마다 가상 스레드를 하나씩 만듭니다.
 * - Thread.sleep()처럼 기다리는 동안 플랫폼 스레드를 오래 붙잡지 않습니다.
 * - 주문 제조 시뮬레이션을 메인 메뉴 흐름과 분리해 백그라운드에서 처리합니다.
 */
public class VirtualThreadOrderProcessor implements AutoCloseable {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OrderService orderService;
    private final ExecutorService executorService;
    private final Set<Integer> processingOrderIds = ConcurrentHashMap.newKeySet();

    public VirtualThreadOrderProcessor(OrderService orderService) {
        this.orderService = orderService;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void startProcessing(int orderId) {
        boolean added = processingOrderIds.add(orderId);

        if (!added) {
            System.out.println("[가상 스레드] 이미 제조 처리 중인 주문입니다. 주문번호: " + orderId);
            return;
        }

        executorService.submit(() -> process(orderId));
    }

    private void process(int orderId) {
        try {
            log(orderId, "가상 스레드 제조 작업 시작");

            orderService.updateOrderStatus(id(orderId), OrderStatus.MAKING);
            sleep(1500);

            orderService.updateOrderStatus(id(orderId), OrderStatus.READY);
            log(orderId, "제조 완료. READY 상태로 변경되었습니다.");
        } catch (OrderNotFoundException e) {
            log(orderId, "주문을 찾지 못해 제조 작업을 종료합니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log(orderId, "프로그램 종료로 제조 작업이 중단되었습니다.");
        } finally {
            processingOrderIds.remove(orderId);
        }
    }

    private int id(int orderId) {
        return orderId;
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private void log(int orderId, String message) {
        System.out.printf("[가상 스레드 %s] 주문번호 %d - %s%n", LocalTime.now().format(FORMATTER), orderId, message);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
