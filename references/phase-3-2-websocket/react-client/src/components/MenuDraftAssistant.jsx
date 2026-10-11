import { useEffect, useRef, useState } from 'react';

import Button from './Button.jsx';
import { openMenuDraftSocket } from '../api/menuDraftSocket.js';
import styles from './MenuDraftAssistant.module.css';

/*
 * [Phase 3-2] 메뉴 설명 칸 아래의 AI 초안 도우미. (관리자 메뉴 등록, 수정 화면)
 *
 * [AI 초안 쓰기]를 처음 누를 때 WebSocket 을 열고, 화면을 떠나면 닫는다.
 * 연결이 열려 있는 동안 초안 -> 다시 쓰기 -> 다시 쓰기 ... 를 같은 연결로 주고받는다.
 * 서버가 이 연결의 대화를 기억하므로, 다시 쓰기는 '고칠 점' 한 줄만 보낸다.
 *
 * props
 *   getMenu         : 지금 입력된 메뉴 정보를 돌려주는 함수 { menuName, categoryName, menuPrice, menuIngredients, menuDescription }
 *   onChange        : 설명 칸의 값을 바꾸는 함수. 글자 조각이 올 때마다 지금까지의 초안 전체를 넘겨 호출한다.
 *   onImageProgress : [선택] 이미지 미리보기(Data URL)를 받는 함수. null 이면 미리보기를 지운다.
 *   onImage         : [선택] 완성된 이미지를 File 로 받는 함수. 직접 고른 이미지 파일처럼 그대로 업로드된다.
 *
 * 흐름 : [AI 초안 쓰기] -> handleDraft() -> withSocket() 이 연결을 열고 인증(auth -> auth_ok)
 *        -> 맡겨 둔 draft 를 보낸다 -> delta 마다 onChange 로 설명 칸을 채운다 -> done
 */
export default function MenuDraftAssistant({ getMenu, onChange, onImageProgress, onImage }) {
    // idle(연결 전) -> connecting(연결, 인증 중) -> ready(대기) <-> writing(쓰는 중)
    const [status, setStatus] = useState('idle');
    // 초안을 한 번이라도 받았는가. 받은 뒤에만 '고칠 점' 입력칸을 보여 준다.
    const [hasDraft, setHasDraft] = useState(false);
    // '고칠 점' 입력칸의 글자
    const [instruction, setInstruction] = useState('');
    // 서버가 보낸 오류 문장
    const [error, setError] = useState(null);

    // 연결 객체(openMenuDraftSocket 이 돌려준 것). 화면에 그릴 값이 아니므로 ref 에 둔다.
    const socketRef = useRef(null);
    // 연결, 인증이 끝나면 보낼 첫 요청. (연결은 비동기라 바로 보낼 수 없다)
    const pendingRef = useRef(null);
    // 지금까지 받은 글자 조각을 이어 붙인 초안
    const textRef = useRef('');
    // 연결, 인증이 끝난 뒤 둘 상태 ('writing' 또는 [선택] 이미지의 'imaging')
    const workingRef = useRef('writing');

    // 화면을 떠나면 연결을 닫는다. 서버는 만들던 초안이 있으면 멈춘다.
    useEffect(() => () => socketRef.current?.close(), []);

    // 새 초안, 다시 쓰기를 시작할 때 : 설명 칸을 비우고 '쓰는 중'으로 둔다
    function begin() {
        textRef.current = '';
        onChange('');
        setError(null);
        setStatus('writing');
    }

    // 연결이 없으면 열고, 인증이 끝난 뒤 send 를 실행한다. 이미 열려 있으면 바로 실행한다.
    function withSocket(send, workingStatus = 'writing') {
        // 이미 연결, 인증이 끝났으면 바로 보낸다
        if (socketRef.current && status !== 'idle' && status !== 'connecting') {
            send(socketRef.current);
            return;
        }
        // 아니면 보낼 일을 맡겨 두고 연결을 연다
        pendingRef.current = send;
        workingRef.current = workingStatus;
        setStatus('connecting');
        // [API 호출] openMenuDraftSocket() : 연결을 열고, 서버 메시지의 type 마다 할 일을 넘긴다
        socketRef.current = openMenuDraftSocket({
            // 인증 성공 -> 맡겨 둔 요청을 이제 보낸다
            auth_ok: () => {
                const pending = pendingRef.current;
                pendingRef.current = null;
                // 기다리던 요청이 있으면 바로 보내고 '쓰는 중'으로 둔다.
                setStatus(pending ? workingRef.current : 'ready');
                pending?.(socketRef.current);
            },
            // 글자 조각 -> 이어 붙인 초안 전체로 설명 칸을 바꾼다
            delta: ({ text }) => {
                textRef.current += text;
                onChange(textRef.current);
            },
            // 초안 완성 -> '고칠 점' 입력칸을 보여 주고 대기 상태로
            done: () => {
                setHasDraft(true);
                setStatus('ready');
            },
            // [중단]으로 멈췄다 -> 대기 상태로 (연결은 그대로라 바로 다시 요청할 수 있다)
            cancelled: () => {
                onImageProgress?.(null);
                setStatus('ready');
            },
            // [선택] 이미지 : 중간 미리보기가 오면 보여주고, 완성본은 File 로 바꿔 넘긴다.
            image_progress: ({ data }) => onImageProgress?.(data),
            image: async ({ data }) => {
                // Data URL('data:image/jpeg;base64,...')을 fetch 하면 파일 내용(Blob)을 얻을 수 있다.
                const blob = await (await fetch(data)).blob();
                // 직접 고른 이미지와 같은 File 객체로 만들어 메뉴 등록 폼에 넘긴다
                onImage?.(new File([blob], 'ai-menu.jpg', { type: 'image/jpeg' }));
                onImageProgress?.(null);
                setStatus('ready');
            },
            // 서버의 오류 문장 -> 보여 주고, 쓰는 중이었다면 대기 상태로 (연결, 인증 중이면 그대로 둔다)
            error: ({ message }) => {
                setError(message);
                onImageProgress?.(null);
                setStatus((prev) => (prev === 'writing' || prev === 'imaging' ? 'ready' : prev));
            },
            // 서버가 닫았거나(인증 실패 등) 네트워크가 끊겼다. 다음에 누르면 새로 연결한다.
            close: () => {
                socketRef.current = null;
                pendingRef.current = null;
                setHasDraft(false);
                setStatus('idle');
            },
        });
    }

    // [AI 초안 쓰기]
    function handleDraft() {
        // 폼에 지금 입력된 메뉴 정보를 받아 온다 (props 의 getMenu)
        const menu = getMenu();
        if (!menu.menuName.trim()) {
            setError('메뉴명을 먼저 입력해주세요.');
            return;
        }
        // [보조 함수] begin() : 설명 칸을 비우고 '쓰는 중'으로
        begin();
        // [보조 함수] withSocket() : 연결, 인증이 끝나면 draft 메시지를 보낸다
        withSocket((socket) => socket.draft(menu));
    }

    // [선택] 메뉴 사진 만들기. 설명 칸의 글은 건드리지 않는다.
    function handleImage() {
        const menu = getMenu();
        if (!menu.menuName.trim()) {
            setError('메뉴명을 먼저 입력해주세요.');
            return;
        }
        setError(null);
        setStatus('imaging');
        withSocket((socket) => socket.image(menu), 'imaging');
    }

    // [다시 쓰기]
    function handleRevise() {
        if (!instruction.trim()) {
            return;
        }
        begin();
        // 고칠 점 한 줄만 보낸다. 메뉴 정보와 직전 초안은 서버가 이 연결의 대화로 기억하고 있다.
        withSocket((socket) => socket.revise(instruction.trim()));
        setInstruction('');
    }

    // 연결 중, 쓰는 중, 사진 만드는 중에는 버튼 자리에 [중단]을 보여 주고 입력을 막는다
    const busy = status === 'connecting' || status === 'writing' || status === 'imaging';

    return (
        <div className={styles.assistant}>
            <div className={styles.row}>
                {busy ? (
                    // [중단] : cancel 메시지를 보낸다. 연결은 그대로 두므로 멈춘 뒤 바로 다시 요청할 수 있다.
                    <Button size="small" variant="secondary" onClick={() => socketRef.current?.cancel()}>
                        {status === 'connecting' ? '연결하는 중…' : '중단'}
                    </Button>
                ) : (
                    <>
                        <Button size="small" variant="secondary" onClick={handleDraft}>
                            {hasDraft ? 'AI 초안 새로 쓰기' : 'AI 초안 쓰기'}
                        </Button>
                        {/* [선택] 이미지 만들기 */}
                        {onImage && (
                            <Button size="small" variant="secondary" onClick={handleImage}>
                                AI 사진 만들기
                            </Button>
                        )}
                    </>
                )}
                <span className={`${styles.status} caption1`}>
                    {status === 'writing' && 'AI 가 쓰는 중…'}
                    {status === 'imaging' && 'AI 가 사진을 만드는 중… (10초쯤 걸린다)'}
                    {status === 'ready' && 'AI 연결됨'}
                </span>
            </div>

            {/*
             * 다시 쓰기는 AI 가 마지막으로 쓴 초안을 고친다. (직접 고친 내용은 서버가 모른다)
             * 주의. 이 도우미는 메뉴 등록 <form> 안에 들어간다. 여기에 <form> 을 또 만들면(중첩 form)
             *      [다시 쓰기]가 바깥 form 까지 제출해 메뉴가 저장돼 버린다. 그래서 form 대신 div 와 버튼을 쓴다.
             */}
            {hasDraft && (
                <div className={styles.row}>
                    <input
                        className={`${styles.input} body2`}
                        value={instruction}
                        onChange={(event) => setInstruction(event.target.value)}
                        onKeyDown={(event) => {
                            // 입력칸에서 Enter 를 누르면 브라우저가 바깥 form 을 제출하므로 막고, 다시 쓰기를 한다.
                            if (event.key === 'Enter') {
                                event.preventDefault();
                                handleRevise();
                            }
                        }}
                        placeholder="고칠 점 (예: 더 짧게, 매운맛을 강조해서)"
                        maxLength={200}
                        disabled={busy}
                        aria-label="고칠 점"
                    />
                    <Button size="small" onClick={handleRevise} disabled={busy || !instruction.trim()}>
                        다시 쓰기
                    </Button>
                </div>
            )}

            {error && <p className={`${styles.error} caption1`}>{error}</p>}
        </div>
    );
}
