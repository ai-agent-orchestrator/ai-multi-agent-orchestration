package com.ohgiraffers.restaurant.ai.websocket;

import com.ohgiraffers.restaurant.ai.dto.ChatMessage;
import com.ohgiraffers.restaurant.ai.dto.MenuDraftMessage;
import com.ohgiraffers.restaurant.ai.service.MenuDraftService;
import com.ohgiraffers.restaurant.ai.service.MenuImageService;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import com.ohgiraffers.restaurant.common.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/* 설명. [Phase 3-2] 관리자 메뉴 설명 초안 WebSocket (ws://localhost:8080/ws/admin/menu-draft)
 *  ----------------------------------------------------------------------------------
 *  연결 하나가 열려 있는 동안 관리자와 서버가 서로 아무 때나 메시지를 보낸다.
 *      관리자 -> 서버 : auth, draft, revise, cancel   (MenuDraftMessage)  + [선택] image
 *      서버 -> 관리자 : auth_ok, delta, done, cancelled, error           + [선택] image_progress, image
 *  ----------------------------------------------------------------------------------
 *  SSE 대신 WebSocket 을 쓰는 이유
 *  - 초안이 만들어지는 '도중에' 관리자가 [중단]을 보낼 수 있어야 한다. 서버 -> 브라우저 한 방향인 SSE 로는 안 된다.
 *  - 연결 하나로 초안 -> 고쳐 쓰기 -> 고쳐 쓰기 ... 를 계속 주고받는다. 서버가 연결마다 대화를 기억한다.
 *  ----------------------------------------------------------------------------------
 *  인증 : 브라우저의 WebSocket 은 연결할 때 Authorization 헤더를 붙일 수 없다.
 *  주소에 ?token= 으로 붙이면 서버 로그, 브라우저 기록에 토큰이 남으므로,
 *  연결한 뒤 첫 메시지(auth)로 토큰을 보내게 하고 여기서 Phase 2 의 JwtTokenProvider 로 검증한다.
 *  ----------------------------------------------------------------------------------
 *  호출 흐름 : 핸들러 -> MenuDraftService(또는 MenuImageService) -> OpenAiClient
 *  글자 조각은 반대 방향으로 돌아온다. OpenAiClient 가 조각마다 람다를 호출하고, 그 람다가 이 클래스의 sendDelta() 다.
 *  주석의 [Service 호출] 은 서비스로 넘어가는 곳, [보조 메서드] 는 이 클래스 안의 메서드를 호출하는 곳이다.
 * */
@Component
public class MenuDraftWebSocketHandler extends TextWebSocketHandler {

    // 세션(연결)마다 따로 보관하는 값의 이름. WebSocketSession.getAttributes() 에 넣는다.
    // 인증한 관리자 (CustomUserDetails)
    private static final String ADMIN = "admin";
    // 이 연결의 초안 대화 기록 (List<ChatMessage>)
    private static final String HISTORY = "history";
    // 지금 초안을 만드는 중인가 (AtomicBoolean)
    private static final String RUNNING = "running";
    // [중단]을 받았는가 (AtomicBoolean)
    private static final String CANCELLED = "cancelled";

    // 첫 메시지(auth)의 토큰을 검증할 때 쓴다. (Phase 2 에서 만든 것을 그대로 쓴다)
    private final JwtTokenProvider jwtTokenProvider;
    // 메뉴 설명 초안을 만드는 서비스. OpenAI 호출은 이 서비스가 OpenAiClient 에 맡긴다.
    private final MenuDraftService menuDraftService;
    // [선택] 메뉴 사진 만들기
    private final MenuImageService menuImageService;
    // 받은 JSON 문자열 <-> DTO, 보낼 Map -> JSON 문자열 변환
    private final ObjectMapper objectMapper;
    // 초안 만들기를 맡길 실행기 (가상 스레드). 이유는 handleTextMessage 의 설명 참고
    private final TaskExecutor taskExecutor;

    /* 설명. 열려 있는 연결 목록 (세션 아이디 -> 세션)
     *  WebSocketSession 은 여러 스레드가 동시에 sendMessage() 를 호출하면 안 된다.
     *  초안 조각은 작업 스레드가, 오류, 중단 응답은 메시지를 받은 스레드가 보낼 수 있으므로,
     *  ConcurrentWebSocketSessionDecorator 로 감싸서 보내기가 차례로 이뤄지게 한다.
     * */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public MenuDraftWebSocketHandler(JwtTokenProvider jwtTokenProvider,
                                     MenuDraftService menuDraftService,
                                     MenuImageService menuImageService,
                                     ObjectMapper objectMapper,
                                     // Spring Boot 가 만들어 둔 비동기 작업용 실행기 (3-1 의 RecommendationService 와 같은 것)
                                     @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.menuDraftService = menuDraftService;
        this.menuImageService = menuImageService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    /* 목차. 1. 연결이 열렸을 때 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        // 보내기 제한 : 한 번 보내는 데 10초, 보내지 못하고 쌓아 둘 수 있는 양 2MB. 넘으면 느린 연결로 보고 끊는다.
        // (이미지 한 장이 Base64 로 약 200KB 라서 넉넉하게 잡았다)
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 10_000, 2 * 1024 * 1024));

        // 이 연결 전용 값을 세션 속성에 만들어 둔다. 핸들러 객체는 모든 연결이 함께 쓰므로 필드에 두면 안 된다.
        // 빈 대화 기록 (MenuDraftService 가 채운다)
        session.getAttributes().put(HISTORY, new ArrayList<ChatMessage>());
        // 아직 만드는 중이 아니다
        session.getAttributes().put(RUNNING, new AtomicBoolean(false));
        // 아직 [중단]을 받지 않았다
        session.getAttributes().put(CANCELLED, new AtomicBoolean(false));

        System.out.println("[MenuDraftWebSocketHandler] 연결됨 : 세션 " + session.getId() + " (아직 인증 전)");
    }

    /* 목차. 2. 메시지를 받았을 때 */
    /* 설명. 한 연결의 메시지는 한 번에 하나씩 차례로 이 메서드에 들어온다.
     *  그래서 여기서 초안을 끝까지 만들면(몇 초) 그동안 같은 연결의 다음 메시지([중단])를 받지 못한다.
     *  초안 만들기는 taskExecutor 의 다른 스레드에 맡기고 이 메서드는 바로 돌아온다.
     * */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        // 보낼 때는 감싸 둔 세션(차례로 보내기)을 쓴다
        WebSocketSession safeSession = sessions.get(session.getId());
        MenuDraftMessage request;
        try {
            // 받은 JSON 문자열을 DTO 로 바꾼다
            request = objectMapper.readValue(message.getPayload(), MenuDraftMessage.class);
        } catch (RuntimeException e) {
            // JSON 이 아니면 오류를 알리고 끝낸다. 연결은 열어 둔다.  -> type: error
            send(safeSession, Map.of("type", "error", "message", "메시지 형식이 올바르지 않습니다."));
            return;
        }

        // type 이 없으면 빈 문자열로 (아래 switch 의 default 로 간다)
        String type = request.getType() == null ? "" : request.getType();
        System.out.println("[MenuDraftWebSocketHandler] 메시지 받음 : type=" + type + " (세션 " + session.getId() + ")");

        // 인증하기 전에는 auth 말고 어떤 메시지도 받지 않는다.
        if (!type.equals("auth") && session.getAttributes().get(ADMIN) == null) {
            // [보조 메서드] closeWithError() : 이유를 알리고 1008 로 연결을 닫는다
            closeWithError(safeSession, "먼저 auth 메시지로 토큰을 보내야 합니다.");
            return;
        }

        // type 에 따라 나눈다. HTTP 의 '주소 + 메서드' 대신 이 값으로 할 일을 구분한다.
        switch (type) {
            // 토큰 검증
            case "auth" -> authenticate(safeSession, request.getToken());
            // 메뉴 정보로 첫 초안 쓰기
            case "draft" -> startDraft(safeSession, request);
            // 고칠 점 한 줄로 다시 쓰기
            case "revise" -> startRevise(safeSession, request.getInstruction());
            // 만들던 것 멈추기
            case "cancel" -> cancel(safeSession);
            // [선택] 메뉴 사진 만들기
            case "image" -> startImage(safeSession, request);
            // 그 밖의 type 은 오류로 알린다. 연결은 열어 둔다.  -> type: error
            default -> send(safeSession, Map.of("type", "error", "message", "알 수 없는 type 입니다 : " + type));
        }
    }

    /* 목차. 3. 인증 (첫 메시지) */
    private void authenticate(WebSocketSession session, String token) {

        try {
            // Phase 2 의 토큰 검증을 그대로 쓴다. 서명, 만료가 틀리면 JwtException
            CustomUserDetails user = jwtTokenProvider.parseAccessToken(token == null ? "" : token);

            // 토큰은 올바르지만 관리자가 아니면 닫는다
            if (!"ROLE_ADMIN".equals(user.getMemberRole())) {
                closeWithError(session, "관리자만 쓸 수 있습니다.");
                return;
            }

            // 이 연결은 인증되었다. 연결이 닫힐 때까지 이 값으로 인증 여부를 판단한다.
            session.getAttributes().put(ADMIN, user);
            System.out.println("[MenuDraftWebSocketHandler] 인증 성공 : " + user.getMemberId());
            // 인증이 끝났다고 알린다. 화면은 이것을 받은 뒤에 draft 를 보낸다.  -> type: auth_ok
            send(session, Map.of("type", "auth_ok"));

        } catch (JwtException | IllegalArgumentException e) {
            // 서명이 틀리거나 만료된 토큰이면 닫는다
            closeWithError(session, "유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }
    }

    /* 목차. 4. 초안 만들기, 고쳐 쓰기 (메시지 받는 스레드에서 실행) */
    private void startDraft(WebSocketSession session, MenuDraftMessage request) {

        // 메뉴 이름이 없으면 무엇에 대한 설명인지 알 수 없다
        if (request.getMenuName() == null || request.getMenuName().isBlank()) {
            send(session, Map.of("type", "error", "message", "메뉴 이름을 먼저 입력해주세요."));
            return;
        }
        // [보조 메서드] run() : 아래 람다(AI 작업)를 작업 스레드에 맡긴다. 람다가 돌려준 Map 이 마지막 메시지가 된다.  -> type: done
        //  [Service 호출] menuDraftService.draft() : 이 연결의 대화 기록과 메뉴 정보로 첫 초안을 만든다.
        //  마지막 인자(delta -> sendDelta(...))는 '조각이 올 때마다 할 일'이다.
        //  이 람다는 MenuDraftService 를 거쳐 OpenAiClient.streamText() 까지 전달되고, 조각마다 거꾸로 호출된다.
        run(session, () -> Map.of("type", "done",
                "text", menuDraftService.draft(history(session), request, delta -> sendDelta(session, delta))));
    }

    private void startRevise(WebSocketSession session, String instruction) {

        // 고칠 점은 한 줄 지시라 200자로 제한한다 (길수록 입력 토큰이 늘어난다)
        if (instruction == null || instruction.isBlank() || instruction.length() > 200) {
            send(session, Map.of("type", "error", "message", "고칠 내용을 200자 이하로 입력해주세요."));
            return;
        }
        // 고칠 초안이 아직 없다
        if (history(session).isEmpty()) {
            send(session, Map.of("type", "error", "message", "먼저 초안을 만들어주세요."));
            return;
        }
        // [보조 메서드] run() : 작업 스레드에 맡긴다.  -> type: done
        //  [Service 호출] menuDraftService.revise() : 대화 기록에 고칠 점을 더해 다시 쓴다. 조각은 startDraft 와 같은 람다로 보낸다.
        run(session, () -> Map.of("type", "done",
                "text", menuDraftService.revise(history(session), instruction, delta -> sendDelta(session, delta))));
    }

    /* 설명. [선택] 메뉴 사진 만들기. 완성본은 'data:image/jpeg;base64,...' 모양(Data URL)으로 보내 화면이 바로 쓸 수 있게 한다. */
    private void startImage(WebSocketSession session, MenuDraftMessage request) {

        if (request.getMenuName() == null || request.getMenuName().isBlank()) {
            send(session, Map.of("type", "error", "message", "메뉴 이름을 먼저 입력해주세요."));
            return;
        }
        // [보조 메서드] run() : 초안과 같은 run() 으로 작업 스레드에 맡긴다. 그래서 [중단], 한 번에 하나만 만들기가 그대로 적용된다.
        run(session, () -> {
            // [Service 호출] menuImageService.generate() : 메뉴 정보로 사진 한 장을 만들어 Base64 로 돌려준다.
            //  두 번째 인자는 중간 미리보기가 올 때마다 할 일이다.
            String base64 = menuImageService.generate(request, partial -> {
                // [중단]을 받았으면 예외를 던져 멈춘다
                checkCancelled(session);
                // 흐릿한 미리보기를 보낸다  -> type: image_progress
                send(session, Map.of("type", "image_progress", "data", "data:image/jpeg;base64," + partial));
            });
            // 이미지는 다 만들어진 뒤에야 오는 경우가 많다. 그사이 [중단]을 눌렀다면 보내지 않는다.
            checkCancelled(session);
            // 완성 이미지를 마지막 메시지로 돌려준다  -> type: image
            return Map.of("type", "image", "data", "data:image/jpeg;base64," + base64);
        });
    }

    /* 설명. AI 작업 하나를 다른 스레드에서 실행하고, 작업이 돌려준 메시지를 보낸다. 한 연결에서는 한 번에 하나만 만든다. */
    private void run(WebSocketSession session, AiJob job) {

        // [Service 호출] isEnabled() : API 키가 있는지만 확인한다. (MenuDraftService 가 OpenAiClient 에 물어본다)
        if (!menuDraftService.isEnabled()) {
            send(session, Map.of("type", "error", "message", "서버에 OPENAI_API_KEY 가 설정되지 않아 AI 기능을 쓸 수 없습니다."));
            return;
        }

        // [보조 메서드] flag() : 이 연결의 '만드는 중' 깃발을 세션 속성에서 꺼낸다
        AtomicBoolean running = flag(session, RUNNING);
        // compareAndSet : false 일 때만 true 로 바꾸고 성공을 알린다. 이미 만드는 중이면 실패한다.
        if (!running.compareAndSet(false, true)) {
            send(session, Map.of("type", "error", "message", "이미 AI 가 작업하고 있습니다. 끝나거나 중단한 뒤 다시 요청해주세요."));
            return;
        }
        // 지난 [중단] 기록을 지운다
        flag(session, CANCELLED).set(false);

        // 작업을 작업 스레드에 넘긴다. 메시지 받는 스레드는 여기서 바로 돌아가 다음 메시지([중단] 등)를 받을 수 있다.
        taskExecutor.execute(() -> {
            // 여기부터는 작업 스레드에서 실행된다. 가상 스레드를 켰다면 VirtualThread[#..] 로 찍힌다.
            System.out.println("[MenuDraftWebSocketHandler] 작업 스레드 : " + Thread.currentThread());
            try {
                // AI 작업(startDraft, startRevise, startImage 가 넘긴 람다)을 실행하고, 돌려준 마지막 메시지를 보낸다  -> type: done / image
                send(session, job.run());
            } catch (DraftCancelledException e) {
                // [중단]으로 멈췄다  -> type: cancelled
                System.out.println("[MenuDraftWebSocketHandler] 중단됨");
                send(session, Map.of("type", "cancelled"));
            } catch (RuntimeException e) {
                // OpenAI 호출 실패 등. 연결은 열어 둔다  -> type: error
                System.out.println("[MenuDraftWebSocketHandler] 실패 : " + e.getMessage());
                send(session, Map.of("type", "error", "message", String.valueOf(e.getMessage())));
            } finally {
                // 성공, 중단, 실패 모두 '만드는 중'을 푼다
                running.set(false);
            }
        });
    }

    /* 설명. 글자 조각 하나를 보낸다. [중단]을 받았거나 연결이 끊겼으면 예외를 던져 OpenAI 스트림 읽기를 멈춘다. */
    private void sendDelta(WebSocketSession session, String delta) {

        // [보조 메서드] checkCancelled() : 멈춰야 하면 여기서 예외가 나고, OpenAiClient.streamText() 의 읽기도 함께 멈춘다
        checkCancelled(session);
        // 조각을 보낸다. 화면은 받은 글자를 설명 칸에 이어 붙인다.  -> type: delta
        send(session, Map.of("type", "delta", "text", delta));
    }

    /* 설명. [중단]을 받았거나 연결이 닫혔으면 예외를 던진다. (작업 스레드가 스스로 멈추게 하는 방법) */
    private void checkCancelled(WebSocketSession session) {
        if (flag(session, CANCELLED).get() || !session.isOpen()) {
            throw new DraftCancelledException();
        }
    }

    /* 목차. 5. 중단 (메시지 받는 스레드에서 실행) */
    /* 설명. 깃발만 세운다. 작업 스레드가 다음 조각을 보내려다 깃발을 보고 스스로 멈춘다. */
    private void cancel(WebSocketSession session) {

        // 만드는 중일 때만 깃발을 세운다
        if (flag(session, RUNNING).get()) {
            flag(session, CANCELLED).set(true);
            System.out.println("[MenuDraftWebSocketHandler] 중단 요청 받음");
        }
    }

    /* 목차. 6. 연결이 닫혔을 때 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        // 만드는 중이었다면 멈춘다. (창을 닫거나 다른 화면으로 가면 연결이 닫힌다)
        Object cancelled = session.getAttributes().get(CANCELLED);
        if (cancelled instanceof AtomicBoolean flag) {
            flag.set(true);
        }
        // 연결 목록에서 뺀다. 세션 속성(대화 기록 등)도 연결과 함께 사라진다.
        sessions.remove(session.getId());

        // 닫힘 코드 : 1000 정상 종료, 1001 새로고침, 탭 닫기, 1008 규칙 위반(인증 실패) 등
        System.out.println("[MenuDraftWebSocketHandler] 연결 닫힘 : 세션 " + session.getId() + " (" + status.getCode() + ")");
    }

    /* 설명. 메시지 하나를 JSON 으로 바꿔 보낸다. 이미 닫힌 연결이면 오류 없이 넘어간다. */
    private void send(WebSocketSession session, Map<String, Object> payload) {

        // 닫힌 연결에는 보내지 않는다
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            // Map 을 JSON 문자열로 바꿔 텍스트 메시지로 보낸다
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException e) {
            // 보내는 사이에 끊겼을 수 있다. 로그만 남긴다
            System.out.println("[MenuDraftWebSocketHandler] 보내기 실패 : " + e.getMessage());
        }
    }

    /* 설명. 이유를 알린 뒤 연결을 닫는다. 1008(POLICY_VIOLATION) 은 '규칙을 어긴 연결'이라는 표준 닫기 코드다. */
    private void closeWithError(WebSocketSession session, String message) {

        System.out.println("[MenuDraftWebSocketHandler] 연결을 닫는다 : " + message);
        // 먼저 이유를 문장으로 알리고  -> type: error
        send(session, Map.of("type", "error", "message", message));
        try {
            // 1008 로 닫는다. 화면은 onclose 의 event.code 로 이유를 구분할 수 있다.
            session.close(CloseStatus.POLICY_VIOLATION.withReason("unauthorized"));
        } catch (IOException ignored) {
            // 이미 닫혔다.
        }
    }

    /* 설명. 이 연결의 대화 기록을 세션 속성에서 꺼낸다. (afterConnectionEstablished 에서 만들어 둔 것) */
    @SuppressWarnings("unchecked")
    private List<ChatMessage> history(WebSocketSession session) {
        return (List<ChatMessage>) session.getAttributes().get(HISTORY);
    }

    /* 설명. 이 연결의 깃발(RUNNING, CANCELLED)을 세션 속성에서 꺼낸다. */
    private AtomicBoolean flag(WebSocketSession session, String name) {
        return (AtomicBoolean) session.getAttributes().get(name);
    }

    /* 설명. AI 작업 (끝나면 화면에 보낼 마지막 메시지를 돌려준다. 예: done, image) */
    @FunctionalInterface
    private interface AiJob {
        Map<String, Object> run();
    }

    /* 설명. [중단]으로 멈췄음을 알리는 내부용 예외 */
    private static class DraftCancelledException extends UncheckedIOException {
        DraftCancelledException() {
            super(new IOException("관리자가 중단했다"));
        }
    }
}
