import { SERVER_URL } from './client.js';
import { useAuthStore } from '../store/authStore.js';

/*
 * [Phase 3-2] 관리자 메뉴 설명 초안 (WebSocket)
 *
 * WebSocket 은 연결 하나를 열어 두고 브라우저와 서버가 서로 아무 때나 메시지를 보낸다.
 * HTTP 요청이 아니므로 axios 도, fetch 도 쓰지 않고 브라우저 내장 WebSocket 을 쓴다.
 * 메시지는 모두 JSON 이고, type 으로 무엇을 하는 메시지인지 구분한다.
 *   보내는 메시지 : auth, draft, revise, cancel  + [선택] image
 *   받는 메시지   : auth_ok, delta, done, cancelled, error  + [선택] image_progress, image
 *
 * 흐름 : MenuDraftAssistant 가 openMenuDraftSocket() 을 호출하며 'type -> 할 일' 함수 묶음(handlers)을 넘긴다.
 *        서버 메시지를 받을 때마다 handlers 의 같은 이름 함수를 호출한다. (aiApi.js 의 SSE 이벤트 처리와 같은 방식)
 *        보낼 때는 이 함수가 돌려준 객체의 draft(), revise(), cancel() 등을 호출한다.
 */

// 개발 서버(npm run dev)에서만 콘솔에 주고받은 메시지를 찍는다
const LOG = import.meta.env.DEV;

// http://localhost:8080 -> ws://localhost:8080 (HTTPS 서버라면 wss:// 가 된다)
const WS_URL = `${SERVER_URL.replace(/^http/, 'ws')}/ws/admin/menu-draft`;

// MenuDraftWebSocketHandler   ws://localhost:8080/ws/admin/menu-draft
/**
 * 연결을 열고, 서버가 보내는 메시지마다 handlers 의 같은 이름 함수를 호출한다.
 *   handlers = { auth_ok, delta, done, cancelled, error, close }
 * 돌려주는 객체의 함수로 메시지를 보낸다.
 */
export function openMenuDraftSocket(handlers) {
    // 핸드셰이크를 시작한다. 연결은 비동기로 열리므로, 이 줄 직후에는 아직 보낼 수 없다.
    const socket = new WebSocket(WS_URL);

    // 브라우저의 WebSocket 은 연결할 때 Authorization 헤더를 붙일 수 없다.
    // 그래서 연결되자마자 첫 메시지로 토큰을 보낸다. 서버가 관리자인지 확인하고 auth_ok 로 응답한다.
    socket.onopen = () => {
        // [보조 함수] send() : 객체를 JSON 문자열로 바꿔 보낸다  -> type: auth
        send({ type: 'auth', token: useAuthStore.getState().accessToken });
    };

    // 서버가 메시지를 보낼 때마다 호출된다
    socket.onmessage = (event) => {
        // 받은 문자열(JSON)을 객체로 바꾼다
        const message = JSON.parse(event.data);
        // 글자 조각(delta)은 너무 많아서 찍지 않는다. 이미지는 Base64 문자열이 길어서 앞부분만 찍는다.
        if (LOG && message.type !== 'delta') {
            console.log(`[소켓 받음] ${message.type}`, message.data ? `${message.data.slice(0, 40)}... (${message.data.length}자)` : message);
        }
        // type 과 같은 이름의 함수가 handlers 에 있으면 호출한다. (MenuDraftAssistant 가 넘겨준 함수)
        handlers[message.type]?.(message);
    };

    // 연결이 끊기면 onerror 다음에 onclose 가 반드시 온다. 끊긴 처리는 onclose 한 곳에서 한다.
    socket.onclose = (event) => {
        // event.code : 닫힘 코드 (1000 정상 종료, 1008 인증 실패 등)
        if (LOG) {
            console.log(`[소켓 닫힘] ${event.code}`, event.reason);
        }
        handlers.close?.(event);
    };

    function send(message) {
        // 아직 열리지 않았거나 이미 닫혔으면 보내지 않는다
        if (socket.readyState !== WebSocket.OPEN) {
            return false;
        }
        if (LOG) {
            // 토큰은 콘솔에 찍지 않는다
            console.log(`[소켓 보냄] ${message.type}`, message.type === 'auth' ? '(토큰)' : message);
        }
        // 객체를 JSON 문자열로 바꿔 보낸다. 서버의 MenuDraftMessage 로 바뀐다.
        socket.send(JSON.stringify(message));
        return true;
    }

    // 화면은 이 함수들로만 보낸다. type 문자열과 JSON.stringify 가 화면 코드에 흩어지지 않는다.
    return {
        // 메뉴 정보를 펼쳐 넣는다  -> type: draft
        draft: (menu) => send({ type: 'draft', ...menu }),
        // 고칠 점 한 줄  -> type: revise
        revise: (instruction) => send({ type: 'revise', instruction }),
        // 만들던 것 멈추기  -> type: cancel
        cancel: () => send({ type: 'cancel' }),
        // [선택] 메뉴 사진 만들기  -> type: image
        image: (menu) => send({ type: 'image', ...menu }),
        // 연결 닫기 (닫힘 코드 1000)
        close: () => socket.close(),
    };
}
