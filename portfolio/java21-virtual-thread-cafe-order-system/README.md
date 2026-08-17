# Cafe Order Management System - Virtual Thread Edition

Java 21 가상 스레드와 JVM 메모리 최적화 포인트를 학습하기 위한 카페 주문 관리 프로그램 변형 버전입니다.

원본 프로젝트는 수정하지 않고, 별도 폴더에 만든 버전입니다.

가상 스레드 리팩토링 흐름도와 실무적 의미는 [`VIRTUAL_THREAD_REFACTORING_GUIDE.md`](VIRTUAL_THREAD_REFACTORING_GUIDE.md)에 정리했습니다.

일반 설계와 Java 21 가상 스레드 설계의 차이는 [`NORMAL_VS_VIRTUAL_THREAD_DESIGN.md`](NORMAL_VS_VIRTUAL_THREAD_DESIGN.md)에 정리했습니다.

AI와 함께 개발하며 발견한 체크 포인트와 회고는 [`AI_DEVELOPMENT_LESSONS.md`](AI_DEVELOPMENT_LESSONS.md)에 정리했습니다.

외국계 기업 포트폴리오 및 영어 설명용 README는 [`README_EN.md`](README_EN.md)에 따로 정리했습니다.

## 원본과 다른 점

- 1번 메뉴는 주문 등록 및 결제까지 처리합니다.
- 주문 등록 및 결제 후 제조 상태 변경을 Java 21 가상 스레드로 비동기 처리합니다.
- 주문 저장 직후 현재 매출 반영 합계를 즉시 보여줍니다.
- 9번 매출 조회는 매출 반영 주문 수, 주문 목록, 합계를 함께 보여줍니다.
- 기능 실행 후 `계속하려면 Enter를 누르세요...`로 결과 확인 시간을 제공합니다.
- `WAITING -> MAKING -> READY` 상태 변경이 백그라운드에서 진행됩니다.
- `OrderRepository`를 동시 접근에 조금 더 안전한 구조로 바꿨습니다.
- Gradle 실행 시 작은 힙 메모리 설정을 적용했습니다.

## 실행 방법

```powershell
.\gradlew.bat run
```

또는 IntelliJ에서 `CafeOrderApplication.main()`을 실행합니다.

## 가상 스레드가 들어간 위치

`VirtualThreadOrderProcessor`에서 사용합니다.

```java
this.executorService = Executors.newVirtualThreadPerTaskExecutor();
```

주문 등록 및 결제 후 다음 코드가 실행됩니다.

```java
virtualThreadOrderProcessor.startProcessing(order.getId());
```

그러면 별도의 가상 스레드가 주문 상태를 백그라운드에서 바꿉니다.

## 메모리 최적화 포인트

작은 콘솔 프로그램이라 실무 서버만큼 큰 효과는 없지만, 다음 최적화 아이디어를 반영했습니다.

- 가상 스레드 사용: 대기 작업마다 무거운 플랫폼 스레드를 만들지 않습니다.
- 작은 JVM 힙 설정: `-Xms32m`, `-Xmx128m`
- 문자열 중복 제거 옵션: `-XX:+UseStringDeduplication`
- 주문 목록 동시 접근 안전성 보강: `CopyOnWriteArrayList`
- 주문 항목은 외부 변경을 막기 위해 복사 후 보관

## 주의

가상 스레드는 네트워크 I/O, DB 호출, 외부 API 호출처럼 기다리는 시간이 긴 작업에서 특히 효과가 큽니다.

이 콘솔 프로그램에서는 실무 효과보다 학습 목적이 큽니다.
