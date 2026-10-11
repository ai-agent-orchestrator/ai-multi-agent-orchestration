import axios from 'axios';

import { useAuthStore } from '../store/authStore.js';

/*
 * Spring 서버 주소는 이 파일 한 곳에만 적는다.
 * 다른 api 파일은 전부 이 axios 인스턴스(api)를 가져다 쓴다.
 */
export const SERVER_URL = 'http://localhost:8080';

export const api = axios.create({
    baseURL: SERVER_URL,
    timeout: 10000,
});

/*
 * 인터셉터(interceptor) :
 * 이 axios 인스턴스(api)를 거치는 모든 요청과 응답을 중간에서 가로채 공통으로 할 일을 끼워 넣는 함수다.
 * 쉽게 말해 이 인터셉터가 없다면 api 함수마다 매번 같은 코드를 반복해서 써야 한다.
 *   - api.interceptors.request.use(...)  : 요청이 서버로 나가기 직전에 실행된다 (아래 요청 로그, 토큰 붙이기)
 *   - api.interceptors.response.use(...) : 응답이 api 함수로 돌아오기 직전에 실행된다 (아래 응답 로그와 오류 변환)
 *
 * 요청·응답 로그 :
 * 브라우저 콘솔(F12 > Console)에 React 의 요청이 어느 주소로 나가고 무엇이 돌아왔는지 찍는다.
 * IntelliJ 콘솔의 Spring 로그와 나란히 놓고 보면 요청 한 줄이 어느 컨트롤러에 도착했는지 짝을 맞출 수 있다.
 * import.meta.env.DEV 는 Vite 가 넣어 주는 값이다. 개발 서버(npm run dev)에서는 true, 배포용 빌드(npm run build)에서는 false 다.
 * 그래서 로그는 개발할 때만 찍히고, 실제 서비스 화면에는 찍히지 않는다.
 */
const LOG = import.meta.env.DEV;

api.interceptors.request.use((config) => {
    // config : 이번에 나갈 요청의 설정 (메서드, 주소, 쿼리스트링, 본문 등)
    if (LOG) {
        console.log(`[요청] ${describe(config)}`, payloadOf(config));
    }
    return config;   // 설정을 돌려줘야 요청이 실제로 나간다
});

/*
 * [Phase 2] 요청 인터셉터 : 모든 요청에 'Authorization: Bearer 토큰' 을 붙인다. (로그인 전에는 붙이지 않는다)
 * 컴포넌트 밖이라 훅을 부를 수 없으므로 getState() 로 스토어를 읽는다.
 */
api.interceptors.request.use((config) => {
    const { accessToken } = useAuthStore.getState();   // 스토어에서 지금 저장된 토큰을 꺼낸다
    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;   // 요청 헤더에 토큰을 붙인다
    }
    return config;
});

/*
 * 서버의 오류 응답은 정상 응답과 모양이 다르다.
 *   - 정상 : ResponseMessage { httpStatus, message, result }
 *   - 오류 : ErrorResponse { code, description, detail }          <- httpStatus 가 없다
 * 컴포넌트가 이 차이를 몰라도 되도록, 오류는 여기서 사람이 읽을 메시지 하나로 바꿔서 던진다.
 * 컴포넌트는 catch 한 err.message 를 화면에 보여주기만 하면 된다.
 */
api.interceptors.response.use(
    // 첫 번째 함수 : 성공 응답(2xx)일 때 실행된다. 로그만 찍고 응답은 손대지 않고 그대로 돌려준다.
    (response) => {
        if (LOG) {
            console.log(`[응답] ${response.status} ${describe(response.config)}`, response.data);
        }
        return response;
    },
    // 두 번째 함수 : 실패 응답(4xx, 5xx)이나 서버에 연결하지 못했을 때 실행된다.
    (error) => {
        // 요청을 취소한 것(AbortController)은 오류가 아니므로 그대로 넘긴다.
        // 보통 Component에서 요청을 취소할 필요가 있을 때 사용한다.
        // (사용자가 응답을 기다리다가 취소 버튼을 누르거나 아예 다른 화면으로 넘어갔을 때)
        if (axios.isCancel(error)) {
            if (LOG) {
                console.log(`[취소] ${describe(error.config)}`);
            }
            return Promise.reject(error);
        }
        if (LOG) {
            console.warn(`[오류] ${error.response?.status ?? '연결 실패'} ${describe(error.config)}`, error.response?.data ?? error.message);
        }

        // [Phase 2] 토큰을 붙였는데 401 이면 만료·위조된 토큰이다. 로그아웃하면 RequireAuth 가 로그인 화면으로 보낸다.
        if (error.response?.status === 401 && error.config?.headers?.Authorization) {
            useAuthStore.getState().logout();
        }

        // 화면에 보여줄 메시지를 담은 Error 로 바꿔서, api 함수를 부른 쪽의 catch 로 던진다.
        return Promise.reject(toApiError(error));
    },
);

// 로그에 찍을 'GET /api/menus' 모양의 문자열을 만든다.
function describe(config) {
    return `${config?.method?.toUpperCase()} ${config?.url}`;
}

// GET 은 쿼리스트링(params)을, POST·PUT 은 본문(data)을 보여준다. FormData 는 [이름, 값] 목록으로 펼친다.
function payloadOf(config) {
    const data = config.data instanceof FormData ? [...config.data.entries()] : hidePassword(config.data);
    return config.params ?? data ?? '';
}

// 로그인 · 회원가입 본문의 비밀번호는 *** 로 가려서 찍는다. (화면 공유나 캡처에 남지 않게)
// 로그에 찍을 사본만 바꾼다. 서버로 가는 본문은 그대로다.
function hidePassword(data) {
    if (data && typeof data === 'object' && 'memberPassword' in data) {
        return { ...data, memberPassword: '***' };
    }
    return data;
}

function toApiError(error) {
    const body = error.response?.data;

    let message;

    if (body?.description) {
        message = body.detail ? `${body.description} - ${body.detail}` : body.description;
    } else if (error.code === 'ERR_NETWORK') {
        message = '서버에 연결할 수 없습니다. Spring 서버가 8080 포트로 실행 중인지 확인하세요.';
    } else {
        message = error.message;
    }

    const apiError = new Error(message);
    apiError.status = error.response?.status;   // 401, 403, 404 등 화면에서 분기가 필요할 때 쓴다
    apiError.code = body?.code;

    return apiError;
}

/*
 * 서버의 정상 응답은 항상 { httpStatus, message, result } 모양이고 실제 데이터는 result 안에 있다.
 * api 함수들은 result 안의 값만 꺼내서 돌려준다. 컴포넌트는 응답 템플릿을 알지 못한다.
 */
export function resultOf(response) {
    return response.data.result;
}

/*
 * 서버는 이미지 주소를 '/uploads/menus/파일명' 처럼 경로만 준다.
 * <img src> 에 넣으려면 서버 주소를 앞에 붙여야 한다. 이미지가 없으면 null 을 그대로 돌려준다.
 * (<img> 태그의 이미지 요청에는 인터셉터가 동작하지 않지만, 이미지는 누구나 볼 수 있게 열어두었다)
 */
export function imageUrl(path) {
    return path ? SERVER_URL + path : null;
}
