import { useActionState } from 'react';
import { Link, useNavigate } from 'react-router';

import { signup } from '../api/authApi.js';
import Button from '../components/Button.jsx';
import Field from '../components/Field.jsx';
import fieldStyles from '../components/Field.module.css';
import styles from './AuthPage.module.css';

export default function SignupPage() {
    const navigate = useNavigate();

    /*
     * 입력 검증은 두 곳에서 한다.
     * - 화면(required, minLength, type="email") : 잘못된 값을 서버에 보내기 전에 바로 알려준다
     * - 서버(@Valid)                             : 화면을 거치지 않은 요청(Postman 등)까지 막는다
     * 화면 검증은 편의이고, 반드시 지켜야 하는 규칙은 서버 검증이다.
     */
    const [state, formAction, isPending] = useActionState(
        async (prevState, formData) => {
            const values = {
                memberId: formData.get('memberId'),
                memberName: formData.get('memberName'),
                memberEmail: formData.get('memberEmail'),
            };
            try {
                await signup({ ...values, memberPassword: formData.get('memberPassword') });
                navigate('/login', { state: { signedUp: true } });
                return { error: null, values };
            } catch (err) {
                // 409(중복 아이디), 400(검증 실패) 모두 서버가 준 설명을 그대로 보여준다.
                return { error: err.message, values };
            }
        },
        { error: null, values: { memberId: '', memberName: '', memberEmail: '' } },
    );

    return (
        <div className={styles.page}>
            <form action={formAction} className={styles.card}>
                <h1 className="title3">회원가입</h1>

                <Field label="아이디" htmlFor="memberId" hint="4~30자">
                    <input
                        id="memberId"
                        name="memberId"
                        className={`${fieldStyles.control} body1`}
                        defaultValue={state.values.memberId}
                        minLength={4}
                        maxLength={30}
                        autoComplete="username"
                        required
                    />
                </Field>

                <Field label="비밀번호" htmlFor="memberPassword" hint="4~30자">
                    <input
                        id="memberPassword"
                        name="memberPassword"
                        type="password"
                        className={`${fieldStyles.control} body1`}
                        minLength={4}
                        maxLength={30}
                        autoComplete="new-password"
                        required
                    />
                </Field>

                <Field label="이름" htmlFor="memberName">
                    <input
                        id="memberName"
                        name="memberName"
                        className={`${fieldStyles.control} body1`}
                        defaultValue={state.values.memberName}
                        maxLength={30}
                        required
                    />
                </Field>

                <Field label="이메일" htmlFor="memberEmail">
                    <input
                        id="memberEmail"
                        name="memberEmail"
                        type="email"
                        className={`${fieldStyles.control} body1`}
                        defaultValue={state.values.memberEmail}
                        autoComplete="email"
                        required
                    />
                </Field>

                {state.error && <p className={`${fieldStyles.error} body2`}>{state.error}</p>}

                <Button type="submit" size="large" disabled={isPending}>
                    {isPending ? '가입하는 중…' : '가입하기'}
                </Button>

                <p className={`${styles.switch} label2`}>
                    이미 회원이신가요? <Link to="/login">로그인</Link>
                </p>
            </form>
        </div>
    );
}
