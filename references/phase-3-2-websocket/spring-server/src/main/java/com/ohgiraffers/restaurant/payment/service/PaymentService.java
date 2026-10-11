package com.ohgiraffers.restaurant.payment.service;

import com.ohgiraffers.restaurant.order.domain.Order;
import com.ohgiraffers.restaurant.payment.domain.Payment;
import com.ohgiraffers.restaurant.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* 설명. 결제 도메인의 서비스
 *  Phase 1의 결제 도메인에는 컨트롤러가 없다. 손님이 결제만 따로 요청하는 화면이 없기 때문이다.
 *  대신 주문 도메인(OrderService)이 주문을 만든 뒤 이 서비스를 호출해 결제를 처리한다.
 *  ----------------------------------------------------------------------------------
 *  도메인끼리 협력하는 예시다. 주문은 "결제가 필요하다"는 것만 알고,
 *  결제를 어떻게 기록하는지는 결제 도메인이 책임진다.
 *  (나중에 실제 결제 대행사 API를 붙이더라도 이 클래스만 바꾸면 된다)
 * */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /* 목차. 1. 결제 처리 */
    /**
     * 주문 금액 전체를 결제한 것으로 기록한다.
     *
     * <p>@Transactional의 기본 전파 속성(REQUIRED) 때문에, OrderService의 트랜잭션 안에서 호출되면
     * 새 트랜잭션을 만들지 않고 주문 트랜잭션에 그대로 참여한다.
     * 따라서 결제 저장이 실패하면 주문 저장도 함께 롤백된다.</p>
     *
     * @param order 결제할 주문
     * @param paymentType 결제 수단 ('CARD' 또는 'CASH')
     * @return 저장된 결제 정보
     */
    @Transactional
    public Payment pay(Order order, String paymentType) {

        Payment payment = paymentRepository.save(new Payment(order, order.getTotalOrderPrice(), paymentType));

        order.completePayment(payment);

        return payment;
    }
}
