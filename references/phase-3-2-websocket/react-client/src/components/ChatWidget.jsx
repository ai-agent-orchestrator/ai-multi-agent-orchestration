import { useState } from 'react';

import ChatPanel from './ChatPanel.jsx';
import { useAuthStore } from '../store/authStore.js';
import styles from './ChatWidget.module.css';

/*
 * [Phase 3] 화면 오른쪽 아래에 떠 있는 AI 메뉴 추천 창.
 *
 * App.jsx 에서 <Routes> 바깥에 한 번만 그린다.
 * 페이지를 옮기면 <Routes> 안의 화면은 사라졌다가 새로 그려지지만, 바깥의 이 컴포넌트는 계속 남는다.
 * 그래서 ChatPanel 의 state(대화 기록)도 그대로 남고, 메뉴 상세를 보러 갔다 와도 대화가 이어진다.
 * 만약 이 창을 페이지 컴포넌트 안에 두었다면 페이지를 옮길 때마다 대화가 사라진다.
 *
 * 창을 닫을 때도 ChatPanel 을 지우지 않고 숨기기만 한다(hidden). 지우면 대화가 사라진다.
 */
export default function ChatWidget() {
    // 창이 열려 있는가
    const [open, setOpen] = useState(false);
    // 로그인한 회원. 바뀌면 아래 ChatPanel 의 key 가 바뀐다.
    const member = useAuthStore((state) => state.member);

    return (
        <div className={styles.widget}>
            {/* 닫아도 지우지 않고 hidden 으로 숨긴다. ChatPanel 의 대화(state)를 남기기 위해서다. */}
            <section className={styles.window} hidden={!open} aria-label="AI 메뉴 추천">
                <header className={styles.top}>
                    <span className="headline2">AI 메뉴 추천</span>
                    <button type="button" className={`${styles.close} label2`} onClick={() => setOpen(false)}>
                        닫기
                    </button>
                </header>
                {/*
                 * key 가 바뀌면 React 는 이 컴포넌트를 버리고 새로 만든다. (state 도 처음부터)
                 * 로그인한 사람이 바뀌면 앞사람의 대화가 남지 않도록 회원코드를 key 로 준다.
                 */}
                <ChatPanel key={member?.memberCode ?? 'guest'} />
            </section>

            {/* 화면 오른쪽 아래의 여닫기 버튼 */}
            <button
                type="button"
                className={`${styles.launcher} label1`}
                onClick={() => setOpen((prev) => !prev)}
                aria-expanded={open}
            >
                {open ? '추천 창 닫기' : 'AI 메뉴 추천'}
            </button>
        </div>
    );
}
