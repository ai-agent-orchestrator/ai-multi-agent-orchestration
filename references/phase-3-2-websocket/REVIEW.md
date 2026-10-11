# phase3-2. WebSocket 으로 관리자 메뉴 설명 초안 붙이기

> 관리자 화면과 서버가 연결 하나로 초안 요청 · 글자 조각 · 중단 · 다시 쓰기를 주고받는 과정을 설명할 수 있다

## 3-1. WebSocket 기초

- [ ] WebSocket 이 HTTP 요청(Upgrade)으로 시작해 `101 Switching Protocols` 뒤에 계속 열려 있는 연결이 된다는 것을 설명할 수 있다
- [ ] 폴링 · SSE · WebSocket 의 통신 방향과 쓰임새의 차이를 설명할 수 있다
- [ ] 메뉴 설명 초안에 SSE 대신 WebSocket 을 쓴 이유(도중에 중단, 연결 하나로 계속 고쳐 쓰기)를 설명할 수 있다
- [ ] WebSocket 에는 주소 · 메서드가 없어서 메시지마다 `type` 으로 종류를 구분한다는 것을 이해할 수 있다

## 3-2. Spring WebSocket

- [ ] `@EnableWebSocket` 과 `WebSocketConfigurer` 로 주소와 핸들러를 등록하고 허용 출처를 정하는 코드를 작성할 수 있다
- [ ] `TextWebSocketHandler` 의 `afterConnectionEstablished` · `handleTextMessage` · `afterConnectionClosed` 가 언제 호출되는지 설명할 수 있다
- [ ] 연결마다 따로 둘 값(인증한 관리자, 대화 기록, 진행 상태)을 `WebSocketSession.getAttributes()` 에 보관하는 코드를 작성할 수 있다
- [ ] 여러 스레드가 한 세션에 동시에 보내면 안 되므로 `ConcurrentWebSocketSessionDecorator` 로 감싸는 이유를 이해할 수 있다
- [ ] 한 연결의 메시지는 차례로 처리되므로, 오래 걸리는 작업을 다른 스레드에 맡겨야 [중단] 메시지를 받을 수 있다는 것을 설명할 수 있다

## 3-3. WebSocket 과 인증

- [ ] 브라우저의 WebSocket 은 연결할 때 `Authorization` 헤더를 붙일 수 없다는 것을 설명할 수 있다
- [ ] 토큰을 주소(`?token=`)에 넣지 않고 첫 메시지(`auth`)로 보내는 이유를 설명할 수 있다
- [ ] 핸드셰이크 주소를 `permitAll()` 로 열고, 핸들러에서 `JwtTokenProvider` 로 토큰과 `ROLE_ADMIN` 을 확인하는 코드를 작성할 수 있다
- [ ] 인증하지 않은 연결을 `1008(POLICY_VIOLATION)` 코드로 닫는 이유를 이해할 수 있다

## 3-4. 서버가 기억하는 대화

- [ ] SSE(3-1)는 React 가 대화를 들고 있고, WebSocket(3-2)은 서버가 연결마다 대화를 들고 있다는 차이를 설명할 수 있다
- [ ] 다시 쓰기 요청이 '고칠 점' 한 줄만으로 동작하는 이유를 설명할 수 있다
- [ ] 중단 · 실패한 요청을 대화 기록에서 되돌려야 하는 이유를 이해할 수 있다
- [ ] `AtomicBoolean` 깃발로 중단을 알리고, 작업 스레드가 다음 조각을 보내기 전에 스스로 멈추는 방식을 설명할 수 있다

## 3-5. React 에서 WebSocket 쓰기

- [ ] `new WebSocket()` 으로 연결하고 `onopen` · `onmessage` · `onclose` 에서 할 일을 나누는 코드를 작성할 수 있다
- [ ] 연결은 비동기이므로 인증(`auth_ok`)이 끝난 뒤 첫 요청을 보내야 한다는 것을 설명할 수 있다
- [ ] 연결 객체를 `useRef` 에 두고, 컴포넌트가 사라질 때 `useEffect` 정리 함수에서 닫는 코드를 작성할 수 있다
- [ ] `<form>` 안에 `<form>` 을 또 만들면 안쪽 버튼이 바깥 form 을 제출해 버리는 문제를 이해할 수 있다

## 3-6. (선택) 이미지 만들기

- [ ] Images API 에 모델 · 프롬프트 · 크기 · 품질을 보내 이미지를 만드는 요청을 이해할 수 있다
- [ ] 완성된 이미지를 Data URL 로 받아 `File` 로 바꾸면 기존 이미지 업로드 흐름을 그대로 쓸 수 있다는 것을 이해할 수 있다
- [ ] 이미지 한 장이 텍스트 응답보다 수십 배 비싸므로 버튼을 누를 때만 만들어야 하는 이유를 이해할 수 있다
