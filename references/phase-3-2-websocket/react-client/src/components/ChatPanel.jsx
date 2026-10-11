import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';

import MenuImage from './MenuImage.jsx';
import { streamRecommendation } from '../api/aiApi.js';
import { useAuthStore } from '../store/authStore.js';
import { formatPrice } from '../format.js';
import styles from './ChatPanel.module.css';

/*
 * [Phase 3] AI 메뉴 추천 채팅. 채팅에 필요한 상태와 동작은 모두 이 컴포넌트 하나에 있다.
 * 떠 있는 창(ChatWidget) 안에 넣었지만, 페이지 하나에 그대로 넣어도 똑같이 동작한다.
 *
 * 대화 기록은 서버가 아니라 이 컴포넌트의 state 에 있다. 서버는 대화를 기억하지 않으므로
 * 질문할 때마다 지금까지의 대화를 함께 보낸다. (aiApi.js 의 MAX_HISTORY 만큼만)
 *
 * 메시지 하나의 모양
 *   { id, role: 'user' | 'assistant', content, filter, candidateCount, menus, error, pending, stopped }
 *
 * 흐름 : [보내기] -> handleSubmit() -> streamRecommendation() (aiApi.js) -> 서버의 SSE 이벤트마다 handlers 의 함수 호출
 *        -> updateLast() 로 마지막 AI 메시지를 고친다 -> 화면이 다시 그려진다
 */
export default function ChatPanel() {
    // 로그인한 회원. 없으면 로그인 안내를 보여 준다.
    const member = useAuthStore((state) => state.member);

    // 화면에 보이는 대화 전체 (서버에 보낼 대화 기록도 여기서 만든다)
    const [messages, setMessages] = useState([]);
    // 입력칸의 글자
    const [input, setInput] = useState('');
    // 지금 AI 응답을 받는 중인가. 받는 동안에는 입력을 막고 [중단] 버튼을 보여 준다.
    const [streaming, setStreaming] = useState(false);

    // [중단]에 쓸 AbortController. 화면을 다시 그릴 때마다 바뀌면 안 되므로 ref 에 둔다.
    const controllerRef = useRef(null);
    // 대화 목록 영역. 새 글자가 올 때마다 맨 아래로 스크롤한다.
    const listRef = useRef(null);

    // messages 가 바뀔 때마다(글자 조각이 붙을 때마다) 목록을 맨 아래로 내린다
    useEffect(() => {
        const list = listRef.current;
        if (list) {
            list.scrollTop = list.scrollHeight;
        }
    }, [messages]);

    // 이 컴포넌트가 사라질 때(로그아웃 등) 진행 중인 요청을 끊는다. 서버도 OpenAI 호출을 멈춘다.
    useEffect(() => () => controllerRef.current?.abort(), []);

    // 맨 마지막 메시지(지금 만들어지고 있는 AI 응답)만 고친다. 새 배열과 새 객체로 바꾼다.
    // patch 는 바꿀 값이 든 객체이거나, 마지막 메시지를 받아 바꿀 값을 돌려주는 함수다.
    function updateLast(patch) {
        setMessages((prev) => {
            // 지금 만들어지고 있는 AI 응답
            const last = prev[prev.length - 1];
            // 함수면 호출해서 바꿀 값을 얻는다 (delta 처럼 앞의 글자에 이어 붙일 때)
            const next = typeof patch === 'function' ? patch(last) : patch;
            // 마지막만 새 객체로 바꾼 새 배열. 기존 배열을 직접 고치면 React 가 바뀐 줄 모른다.
            return [...prev.slice(0, -1), { ...last, ...next }];
        });
    }

    async function handleSubmit(event) {
        // form 제출로 페이지가 새로고침되지 않게 막는다
        event.preventDefault();
        const text = input.trim();
        // 빈 질문이거나 이미 응답을 받는 중이면 보내지 않는다
        if (!text || streaming) {
            return;
        }

        // 서버에 보낼 대화 : 지금까지 주고받은 말(실패한 응답은 빼고) + 새 질문
        const history = [
            ...messages
                .filter((message) => message.content && !message.error)
                // 화면용 값(filter, menus 등)은 빼고 서버의 ChatMessage 모양 { role, content } 만 남긴다
                .map(({ role, content }) => ({ role, content })),
            { role: 'user', content: text },
        ];

        // 화면에는 내 질문과, 글자가 채워질 빈 AI 응답을 먼저 그린다.
        setMessages((prev) => [
            ...prev,
            { id: crypto.randomUUID(), role: 'user', content: text },
            { id: crypto.randomUUID(), role: 'assistant', content: '', pending: true },
        ]);
        setInput('');
        setStreaming(true);

        // 이번 요청을 끊을 때 쓸 리모컨. [중단] 버튼과 화면 정리(useEffect)가 쓸 수 있게 ref 에 둔다.
        const controller = new AbortController();
        controllerRef.current = controller;

        try {
            // [API 호출] streamRecommendation() : 대화를 보내고, 서버가 보내는 이벤트 이름마다 할 일을 정해 넘긴다.
            // 응답이 모두 끝나야(done) 이 await 가 끝난다. 그 전까지는 아래 함수들이 이벤트마다 호출된다.
            await streamRecommendation(history, {
                // 뽑은 조건 -> 조건 칩을 그린다
                filter: (filter) => updateLast({ filter }),
                // 후보 개수 -> '조건에 맞는 메뉴 N개 중에서' 를 보여 준다
                candidates: ({ count }) => updateLast({ candidateCount: count }),
                // 글자 조각 -> 지금까지의 응답 뒤에 이어 붙인다
                delta: ({ text: piece }) => updateLast((last) => ({ content: last.content + piece })),
                // 추천 메뉴 -> 카드를 그린다
                recommended: ({ menus }) => updateLast({ menus }),
                // 서버의 실패 문장 -> 그대로 보여 준다
                error: ({ message }) => updateLast({ error: message }),
            }, controller.signal);
        } catch (error) {
            // [중단]으로 끊었으면 '(중단했어요)', 그 밖의 실패면 오류 문장을 보여 준다
            if (error.name === 'AbortError') {
                updateLast({ stopped: true });
            } else {
                updateLast({ error: error.message });
            }
        } finally {
            // 성공, 중단, 실패 모두 '받는 중' 상태를 푼다
            updateLast({ pending: false });
            setStreaming(false);
            controllerRef.current = null;
        }
    }

    // [중단] : fetch 의 연결을 끊는다. 서버는 다음 이벤트를 보내다 실패하고 작업을 멈춘다.
    function handleStop() {
        controllerRef.current?.abort();
    }

    // [새 대화] : 화면의 대화를 비운다. 서버는 대화를 기억하지 않으므로 이것만으로 처음부터 시작된다.
    function handleReset() {
        setMessages([]);
    }

    // 로그인하지 않았으면 채팅 대신 로그인 안내를 보여 준다 (서버의 추천 API 는 로그인한 사람만 쓸 수 있다)
    if (!member) {
        return (
            <div className={styles.guest}>
                <p className="body2">로그인하면 AI 에게 메뉴를 추천받을 수 있어요.</p>
                <Link to="/login" className={`${styles.loginLink} label1`}>로그인하러 가기</Link>
            </div>
        );
    }

    return (
        <div className={styles.panel}>
            <div className={styles.list} ref={listRef}>
                {messages.length === 0 ? (
                    <p className={`${styles.greeting} body2`}>
                        원하는 메뉴를 편하게 말해 주세요.
                        <br />
                        예) 땅콩 알레르기가 있는데 만원 이하로 매콤한 식사 뭐 있어요?
                    </p>
                ) : (
                    messages.map((message) =>
                        message.role === 'user' ? (
                            <p key={message.id} className={`${styles.user} body2`}>{message.content}</p>
                        ) : (
                            <AssistantMessage key={message.id} message={message} />
                        ),
                    )
                )}
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
                <input
                    className={`${styles.input} body2`}
                    value={input}
                    onChange={(event) => setInput(event.target.value)}
                    placeholder="무엇을 드시고 싶으세요?"
                    maxLength={500}
                    disabled={streaming}
                    aria-label="질문"
                />
                {/* 응답을 받는 동안에는 [보내기] 자리에 [중단] 을 보여 준다 */}
                {streaming ? (
                    <button type="button" className={`${styles.send} label2`} onClick={handleStop}>중단</button>
                ) : (
                    <button type="submit" className={`${styles.send} label2`} disabled={!input.trim()}>보내기</button>
                )}
            </form>

            <button
                type="button"
                className={`${styles.reset} caption1`}
                onClick={handleReset}
                disabled={streaming || messages.length === 0}
            >
                새 대화
            </button>
        </div>
    );
}

/* AI 응답 하나. 뽑은 조건 -> 후보 개수 -> 응답 글자 -> 추천 메뉴 카드 순서로 채워진다. */
function AssistantMessage({ message }) {
    const { filter, candidateCount, content, menus, error, pending, stopped } = message;

    return (
        <div className={styles.assistant}>
            {/* event: filter 를 받으면 나타난다 */}
            {filter && <FilterChips filter={filter} />}
            {/* event: candidates 를 받으면 나타난다 (0개일 수도 있으므로 undefined 와 비교한다) */}
            {candidateCount !== undefined && (
                <span className={`${styles.meta} caption1`}>조건에 맞는 메뉴 {candidateCount}개 중에서 골랐어요</span>
            )}

            {/* event: delta 가 올 때마다 글자가 늘어난다 */}
            {content && <p className={`${styles.answer} body2`}>{content}</p>}
            {pending && !content && <p className={`${styles.meta} caption1`}>응답을 만드는 중...</p>}
            {stopped && <span className={`${styles.meta} caption1`}>(중단했어요)</span>}
            {error && <p className={`${styles.error} caption1`}>{error}</p>}

            {/* event: recommended 를 받으면 추천 카드가 나타난다 */}
            {menus?.length > 0 && (
                <ul className={styles.menus}>
                    {menus.map((menu) => (
                        <li key={menu.menuCode}>
                            {/* 메뉴 상세로 가도 채팅창은 라우트 밖에 있어서 대화가 그대로 남는다. */}
                            <Link to={`/menus/${menu.menuCode}`} className={styles.menu}>
                                <span className={styles.thumb}>
                                    <MenuImage path={menu.menuImageUrl} alt={menu.menuName} compact />
                                </span>
                                <span className={`${styles.menuName} label2`}>{menu.menuName}</span>
                                <span className={`${styles.meta} caption1`}>{formatPrice(menu.menuPrice)}</span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

/* AI 가 손님의 말에서 뽑은 조건. 무엇을 기준으로 골랐는지 손님이 확인할 수 있게 보여준다. */
function FilterChips({ filter }) {
    // 서버의 MenuFilter 네 필드를 칩 글자로 바꾼다. 값이 없는 것(null, 빈 배열)은 빠진다.
    const chips = [
        filter.categoryName,
        filter.maxPrice && `${formatPrice(filter.maxPrice)} 이하`,
        ...filter.excludeAllergens.map((name) => `${name} 제외`),
        ...filter.keywords,
    ].filter(Boolean);

    return (
        <div className={styles.chips}>
            {chips.length === 0 ? (
                <span className={`${styles.chip} caption2`}>조건 없음</span>
            ) : (
                chips.map((chip) => <span key={chip} className={`${styles.chip} caption2`}>{chip}</span>)
            )}
        </div>
    );
}
