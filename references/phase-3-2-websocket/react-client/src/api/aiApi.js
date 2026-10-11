import { SERVER_URL } from './client.js';
import { useAuthStore } from '../store/authStore.js';

/*
 * [Phase 3] AI 메뉴 추천 (SSE 스트리밍)
 *
 * 이 파일만 axios 가 아니라 fetch 를 쓴다.
 * - 브라우저의 axios 는 응답을 다 받은 뒤에야 돌려주므로, 글자가 조금씩 도착하는 모습을 보여줄 수 없다.
 * - 브라우저의 EventSource 는 SSE 전용이지만 GET 만 보낼 수 있고 Authorization 헤더도 붙이지 못한다.
 * - fetch 는 POST, 헤더를 자유롭게 쓰면서, 응답 본문(response.body)을 스트림으로 조금씩 읽을 수 있다.
 * 그래서 axios 인스턴스(client.js)의 인터셉터가 해 주던 일(토큰 붙이기, 401 처리, 로그)을 여기서 직접 한다.
 *
 * 흐름 : ChatPanel 이 streamRecommendation() 을 호출하며 '이벤트 이름 -> 할 일' 함수 묶음(handlers)을 넘긴다.
 *        이 파일은 서버의 SSE 이벤트를 읽을 때마다 handlers 의 같은 이름 함수를 호출한다.
 *        (서버의 OpenAiClient.streamText() 가 조각마다 onDelta 를 호출하는 것과 같은 방식이다)
 */

// 개발 서버(npm run dev)에서만 콘솔에 요청, 응답 로그를 찍는다
const LOG = import.meta.env.DEV;

// 서버에 보낼 최근 대화 개수. 서버도 같은 수만 쓰고 나머지는 버린다. (오래된 대화까지 보내면 토큰 비용만 늘어난다)
export const MAX_HISTORY = 10;

// RecommendationController.recommend()   POST /api/ai/recommendations
/**
 * 대화를 보내고, 서버가 보내는 SSE 이벤트마다 handlers 의 같은 이름 함수를 호출한다.
 *   handlers = { filter, candidates, delta, recommended, done, error }
 * 이벤트를 모두 받으면 돌아온다. signal 로 중간에 끊을 수 있다(AbortController).
 */
export async function streamRecommendation(messages, handlers, signal) {
    // 로그인할 때 저장해 둔 토큰을 꺼낸다 (axios 인터셉터가 하던 일)
    const { accessToken } = useAuthStore.getState();

    if (LOG) {
        console.log('[요청] POST /api/ai/recommendations (스트리밍)', messages.at(-1)?.content);
    }

    let response;
    try {
        // 요청을 보낸다. 서버가 응답을 '시작'하면 돌아온다. (본문은 아직 다 오지 않았다)
        response = await fetch(`${SERVER_URL}/api/ai/recommendations`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // 정상이면 SSE, 검증 실패 같은 오류면 JSON 으로 온다. 둘 다 받겠다고 알린다.
                Accept: 'text/event-stream, application/json',
                // 토큰을 직접 붙인다 (axios 인터셉터가 하던 일)
                Authorization: `Bearer ${accessToken}`,
            },
            // 본문 : { messages: [{ role, content }, ...] } -> 서버의 RecommendationRequest
            body: JSON.stringify({ messages: messages.slice(-MAX_HISTORY) }),
            // [중단]을 누르면 이 signal 로 연결을 끊는다
            signal,
        });
    } catch (error) {
        // [중단]으로 끊은 것은 오류가 아니다. 호출한 쪽이 구분할 수 있게 그대로 던진다.
        if (error.name === 'AbortError') {
            throw error;
        }
        // cause 에 원래 오류를 담아 두면, 콘솔에서 진짜 원인(네트워크 오류 등)을 함께 볼 수 있다.
        throw new Error('서버에 연결할 수 없습니다. Spring 서버가 8080 포트로 실행 중인지 확인하세요.', { cause: error });
    }

    // SSE 를 열기 전에 실패한 경우(400, 401, 503)는 평소처럼 JSON 오류 응답이 온다.
    if (!response.ok) {
        // 오류 본문(JSON)을 읽는다. JSON 이 아니면 null
        const body = await response.json().catch(() => null);
        if (LOG) {
            console.warn(`[오류] ${response.status} POST /api/ai/recommendations`, body);
        }
        // 토큰을 붙였는데 401 이면 만료, 위조된 토큰이다. client.js 의 인터셉터와 같은 처리를 한다.
        if (response.status === 401) {
            useAuthStore.getState().logout();
        }
        // 화면에 보여 줄 문장으로 바꿔 던진다. (서버의 ErrorResponse : description, detail)
        const error = new Error(body?.detail ? `${body.description} - ${body.detail}` : `요청 실패 (${response.status})`);
        error.status = response.status;
        throw error;
    }

    /*
     * 응답 본문을 글자 스트림으로 읽는다.
     * 서버가 보내는 SSE 는 이런 덩어리가 빈 줄(\n\n)로 구분되어 이어진다.
     *   event:delta
     *   data:{"text":"오늘은"}
     * 네트워크는 덩어리 경계를 지켜 주지 않는다. 한 번에 읽은 글자 안에 덩어리가 반쯤 걸쳐 있을 수 있으므로,
     * 빈 줄까지 온 덩어리만 처리하고 남은 조각은 buffer 에 두었다가 다음에 읽은 글자와 이어 붙인다.
     */
    // 바이트를 글자(UTF-8)로 바꿔 읽는 도구. 한글 한 글자가 두 번에 나뉘어 도착해도 깨지지 않는다.
    const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
    // 아직 처리하지 못한 글자를 모아 두는 곳
    let buffer = '';
    // 받은 글자 조각 수 (로그용)
    let deltaCount = 0;

    while (true) {
        // 도착한 글자를 읽는다. 아직 오지 않았으면 올 때까지 기다린다.
        const { value, done } = await reader.read();
        // 서버가 응답을 끝냈다 (emitter.complete())
        if (done) {
            break;
        }
        // 줄바꿈을 \n 하나로 맞춰 이어 붙인다
        buffer += value.replaceAll('\r\n', '\n');

        // 빈 줄을 기준으로 이벤트 덩어리를 나눈다
        const blocks = buffer.split('\n\n');
        // 마지막은 아직 끝나지 않은 덩어리일 수 있으므로 buffer 에 남겨 둔다
        buffer = blocks.pop();

        for (const block of blocks) {
            // [보조 함수] parseBlock() : 'event:' 줄과 'data:' 줄을 꺼낸다
            const { event, data } = parseBlock(block);
            if (!event) {
                continue;
            }
            // delta 는 수십 번 오므로 개수만 세고, 나머지 이벤트만 콘솔에 찍는다
            if (event === 'delta') {
                deltaCount++;
            } else if (LOG) {
                console.log(`[응답] event:${event}`, data);
            }
            // 이벤트 이름과 같은 이름의 함수가 handlers 에 있으면 호출한다. (ChatPanel 이 넘겨준 함수)
            handlers[event]?.(data);
        }
    }

    if (LOG) {
        console.log(`[응답] 스트림 끝 (delta ${deltaCount}개)`);
    }
}

// 'event:이름' 과 'data:JSON' 줄을 꺼낸다. (콜론 뒤의 공백 한 칸은 있어도 되고 없어도 된다)
function parseBlock(block) {
    let event = null;
    let data = '';
    for (const line of block.split('\n')) {
        if (line.startsWith('event:')) {
            // 이벤트 이름 (filter, candidates, delta, recommended, done, error)
            event = line.slice('event:'.length).trim();
        } else if (line.startsWith('data:')) {
            // 보낸 내용 (JSON 문자열)
            data += line.slice('data:'.length).replace(/^ /, '');
        }
    }
    // data 는 JSON 문자열이므로 객체로 바꿔 돌려준다
    return { event, data: data ? JSON.parse(data) : null };
}
