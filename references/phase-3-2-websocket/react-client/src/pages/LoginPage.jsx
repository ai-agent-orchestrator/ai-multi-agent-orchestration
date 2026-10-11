import { useActionState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';

import { login } from '../api/authApi.js';
import Button from '../components/Button.jsx';
import Field from '../components/Field.jsx';
import { useAuthStore } from '../store/authStore.js';
import fieldStyles from '../components/Field.module.css';
import styles from './AuthPage.module.css';

export default function LoginPage() {
    const saveLogin = useAuthStore((state) => state.login);
    const navigate = useNavigate();
    const location = useLocation();

    // 다른 화면에서 로그인하러 왔으면 끝나고 그 화면으로 돌아간다.
    const from = location.state?.from ?? '/menus';
    const signedUp = location.state?.signedUp;

    /*
     * React 19 의 form Actions. (앱 4 에서 다룬 방식)
     * <form action={formAction}> 으로 제출하면 React 가 입력값을 FormData 로 모아 이 함수에 넘겨준다.
     * isPending 은 이 함수가 끝날 때까지 true 다. (버튼 비활성화에 쓴다)
     *
     * 폼은 제출이 끝나면 입력값이 비워진다. 실패했을 때 아이디가 남아 있도록
     * 입력했던 아이디를 상태로 돌려주고 defaultValue 로 다시 채운다.
     */
    const [state, formAction, isPending] = useActionState(
        async (prevState, formData) => {
            const memberId = formData.get('memberId');
            try {
                const { accessToken, member } = await login({ memberId, memberPassword: formData.get('memberPassword') });
                // [Phase 2] 서버가 발급한 토큰을 저장한다. 이후 요청마다 client.js 의 인터셉터가 헤더에 붙인다.
                saveLogin(accessToken, member);
                navigate(from, { replace: true });
                return { error: null, memberId };
            } catch (err) {
                return { error: err.message, memberId };
            }
        },
        { error: null, memberId: '' },
    );

    return (
        <div className={styles.page}>
            <form action={formAction} className={styles.card}>
                <h1 className="title3">로그인</h1>

                {signedUp && <p className={`${styles.notice} label1`}>회원가입이 완료되었습니다. 로그인해 주세요.</p>}

                <Field label="아이디" htmlFor="memberId">
                    <input
                        id="memberId"
                        name="memberId"
                        className={`${fieldStyles.control} body1`}
                        defaultValue={state.memberId}
                        autoComplete="username"
                        required
                    />
                </Field>

                <Field label="비밀번호" htmlFor="memberPassword">
                    <input
                        id="memberPassword"
                        name="memberPassword"
                        type="password"
                        className={`${fieldStyles.control} body1`}
                        autoComplete="current-password"
                        required
                    />
                </Field>

                {state.error && <p className={`${fieldStyles.error} body2`}>{state.error}</p>}

                <Button type="submit" size="large" disabled={isPending}>
                    {isPending ? '로그인하는 중…' : '로그인'}
                </Button>

                <p className={`${styles.hint} caption1`}>
                    테스트 계정 : user01 / user1234 (손님), admin / admin1234 (관리자)
                </p>
                <p className={`${styles.switch} label2`}>
                    아직 회원이 아니신가요? <Link to="/signup">회원가입</Link>
                </p>
            </form>
        </div>
    );
}
