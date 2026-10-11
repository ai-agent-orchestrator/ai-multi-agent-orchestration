# 오지라퍼 레스토랑 (Phase 3-2) — 에이전트 지침

Spring Boot REST API 서버(`../spring-server`)에 붙는 React 클라이언트다.
디자인 시스템(Montage)에서 뽑은 토큰으로 화면을 만든다.

이 지침은 **두 층**으로 나뉜다. 연동할 서버가 바뀌면 1층은 그대로 두고 **2층만 바꾼다.**

| 층 | 들어가는 절 | 결정하는 것 |
|---|---|---|
| **1. 화면 규칙** | 사용 라이브러리 · 폴더 구조 · 상태 · 스타일 규칙 · 확인 | 디자인 토큰 |
| **2. 서버 통신 규칙** | 서버 · 데이터 요청 · 응답 템플릿 · 명세가 빠뜨린 것 | API 명세(`api-docs.json`) |

---

# 1층. 화면 규칙

## 사용 라이브러리

```
- 앱이 쓰는 라이브러리는 react, react-dom, react-router, axios, zustand 만 쓴다.
- UI 라이브러리와 CSS 프레임워크는 도입하지 않는다.
- TypeScript 문법을 쓰지 않는다.
```

목록에 없는 것은 도입하지 않는다. `fetch`는 브라우저 내장 기능이라 이 목록과 무관하다.

## 무엇으로 요청하나

```
- 한 번에 받고 끝나는 요청(JSON 한 덩어리)은 axios 를 쓴다.
- 응답을 조금씩 받아 화면에 흘려야 하는 요청은 fetch 를 쓴다.
  예: LLM 이 토큰을 하나씩 내려주는 응답
```

브라우저의 axios는 응답을 스트림으로 주지 않는다. 지금은 AI 메뉴 추천(`api/aiApi.js`)만 fetch를 쓰고, 나머지는 모두 axios다.
관리자 메뉴 설명 초안은 HTTP 가 아니라 WebSocket(`api/menuDraftSocket.js`)이다.

## 폴더 구조

```
src/
    api/         서버 요청. 컴포넌트는 이 파일만 호출한다
    components/  주소에 직접 대응하지 않는 화면 조각
    pages/       주소 하나에 대응하는 화면 (관리자 화면은 pages/admin/)
    store/       여러 화면이 함께 쓰는 값 (Zustand)

- 주소에 직접 대응하면 pages, 아니면 components 에 둔다.
- 서버 도메인(회원·메뉴·장바구니·주문·관리자·AI)마다 api 파일을 하나씩 둔다.
```

## 상태

```
- 화면 하나에서만 쓰는 값은 useState 에 둔다.
- 검색어, 카테고리, 페이지 번호는 state 가 아니라 URL 쿼리스트링에 둔다.
- 여러 화면이 공유하는 값(로그인 회원, 장바구니 개수)은 Zustand 스토어에 둔다.
- 서버에서 받아 온 목록을 전역 스토어에 넣지 않는다. 받은 화면이 갖는다.
- 컴포넌트 밖(axios 인터셉터 등)에서 스토어를 읽을 때는 훅 대신 useXxxStore.getState() 를 쓴다.
- 페이지를 옮겨도 남아 있어야 하는 화면(떠 있는 AI 추천 창)은 App.jsx 의 <Routes> 바깥에 둔다.
  그러면 컴포넌트가 사라지지 않아 useState 만으로 값(대화 기록)이 유지된다. 전역 스토어에 넣지 않는다.
```

## 스타일 규칙

```
- 색은 반드시 var(--토큰) 으로 쓴다. 색 값을 직접 적지 않는다.
- font-size 를 직접 쓰지 않는다. 타입 스타일 클래스(.title2, .body1 등)를 쓴다.
- 간격은 --space-* 스케일, 모서리는 --radius-* 안에서만 고른다.
- 스타일은 컴포넌트마다 CSS Module(*.module.css)에 둔다.
- 토큰 목록은 design/montage.tokens.json 을 참조한다.
```

- `src/tokens.css`는 `npm run tokens`가 만드는 생성물이다. **직접 편집하지 않는다.**
- 입력 요소의 모양은 `components/Field.module.css`의 `control` 클래스를 가져다 쓴다.
- 이미지가 있을 수도 없을 수도 있는 자리는 `components/MenuImage.jsx`를 쓴다.

## 코드 규약

```
- 들여쓰기는 스페이스 4칸이다.
- 주석은 한글로 쓴다. 이모지를 쓰지 않는다.
- 함수 컴포넌트만 쓴다. 상태는 직접 고치지 않고 새 배열·객체로 바꾼다.
- 목록에는 데이터의 고유 값으로 key 를 준다.
```

## 확인

```bash
npm run lint
npm run build
```

둘 다 통과해도 **화면은 브라우저에서 직접 확인한다.** 린트와 빌드는 코드를 실행해 보지 않는다.

---

# 2층. 서버 통신 규칙

명세 원본은 `api-docs.json`(Spring 서버의 `/v3/api-docs`를 저장한 것)이다.
Swagger UI는 서버를 띄운 뒤 `http://localhost:8080/swagger-ui.html`에서 본다.

## 서버

```
- 주소는 http://localhost:8080 이다. baseURL 은 api/client.js 의 axios 인스턴스 한 곳에만 적는다.
- 서버의 CORS 설정은 http://localhost:5173 만 허용한다. vite.config.js 의 strictPort 를 지운다거나 포트를 바꾸지 않는다.
- 서버 주소는 모두 /api 로 시작한다. 관리자 전용은 /api/admin 으로 시작한다.
- 요청·응답 로그는 api/client.js 의 인터셉터가 개발 모드에서만 찍는다. api 함수나 컴포넌트에 console.log 를 따로 넣지 않는다.
  예외로 fetch 를 쓰는 api/aiApi.js 는 인터셉터를 거치지 않으므로 같은 형식([요청] [응답] [오류])의 로그를 직접 찍는다.
```

## 데이터 요청

```
- 요청 함수 위에는 이 요청을 받는 컨트롤러 메서드를 주석으로 적는다.
  예) // MenuController.findMenus()   GET /api/menus
- 페이지 번호는 1부터 센다. 응답의 number 도 1부터다.
- 요청 주소는 menuCode 로 조립한다. 서버가 링크를 주지 않는다. 내 정보·내 주문·장바구니는 /me 로 요청한다.
- 이미지 주소(menuImageUrl)는 '/uploads/menus/파일명' 처럼 경로만 온다.
  <img> 에 넣을 때는 api/client.js 의 imageUrl() 로 서버 주소를 붙인다. null 이면 이미지가 없는 것이다.
- 메뉴 등록·수정(/api/admin/menus)은 JSON 이 아니라 FormData 로 보낸다.
  Content-Type 을 손으로 적지 않는다. (axios 가 boundary 를 붙여야 한다)
  알레르기 코드는 같은 이름(allergenCodes)으로 여러 번 append 한다. 수정할 때는 최종 목록 전체를 보낸다.
- 주문 요청에 가격을 넣지 않는다. 서버가 계산한다.
- 장바구니는 서버(DB)에 있다. 담기 · 수량 변경 · 빼기의 응답은 바뀐 장바구니 전체(cartItems)다.
  화면은 받은 목록으로 다시 그리고, 헤더 배지 개수는 store/cartStore.js 의 setCountFrom 으로 갱신한다.
```

## 응답 템플릿

정상 응답과 오류 응답의 모양이 다르다.

```json
{ "httpStatus": 200, "message": "메뉴 목록 조회 성공", "result": { "menuPage": { "content": [] } } }
```

```json
{ "code": "ERROR_CODE_00001", "description": "조회 실패", "detail": "해당 메뉴가 존재하지 않습니다. menuCode=999" }
```

```
- api/ 함수는 result 안의 값만 꺼내 반환한다. 컴포넌트가 응답 템플릿을 알지 않게 한다.
- 오류는 api/client.js 의 인터셉터가 'description - detail' 메시지를 가진 Error 로 바꿔 던진다.
  컴포넌트는 err.message 를 보여주고, 분기가 필요하면 err.status(404 등)를 본다.
- 오류 응답에는 httpStatus 가 없다.
- 삭제 응답은 HTTP 200 이다. (204 가 아니다)
- orderableStatus 는 'Y' 또는 'N', paymentType 은 'CARD' 또는 'CASH', memberRole 은 'ROLE_USER' 또는 'ROLE_ADMIN' 이다.
```

## 명세가 빠뜨린 것

`api-docs.json`만 읽어서는 알 수 없는 것들이다.

```
- result 는 Map 이라 명세에 내부 구조가 없다. 각 API 의 description 에 적힌 키 이름을 따른다.
- ErrorResponse 스키마가 없다. 위 오류 응답 템플릿을 따른다.
- 최상위 카테고리(1 식사, 2 음료, 3 디저트)는 refCategoryCode 가 null 이다.
  메뉴 목록을 상위 카테고리 코드로 조회하면 하위 카테고리의 메뉴까지 함께 온다.
- 손님용 메뉴 목록(/api/menus)은 주문 가능한 메뉴만 준다. 관리자용(/api/admin/menus)은 주문 불가 메뉴도 준다.
- 삭제된 메뉴는 목록과 상세 어디에도 나오지 않는다(상세는 404).
```

## 인증 (Spring Security + JWT)

```
- 로그인 응답의 result.accessToken 과 result.member 를 store/authStore.js 에 함께 저장한다.
- 토큰은 api/client.js 의 요청 인터셉터 한 곳에서만 붙인다. (Authorization: Bearer {토큰})
  api 함수나 컴포넌트에서 헤더를 따로 붙이지 않는다. 인터셉터는 useAuthStore.getState() 로 토큰을 읽는다.
  예외로 fetch 를 쓰는 api/aiApi.js 는 같은 방법(getState)으로 토큰을 읽어 직접 붙이고, 401 이면 직접 로그아웃시킨다.
- 요청 본문과 주소에 memberCode 를 넣지 않는다. 서버가 토큰에서 회원을 꺼낸다. (내 정보·내 주문·장바구니는 /me)
- 401 : 토큰이 없거나 만료·위조됨. 토큰을 붙인 요청이 401 이면 인터셉터가 로그아웃시킨다.
- 403 : 로그인은 했지만 권한이 없음 (손님 토큰으로 /api/admin/** 요청). 오류 메시지를 보여준다.
- 누구나 쓰는 API(POST /api/auth/**, GET /api/menus/**, /api/categories, /api/allergens, 이미지)는 토큰이 없어도 된다.
- memberRole 은 화면을 가리는 데만 쓴다. 권한 판단은 서버가 토큰으로 한다.
```

## AI 메뉴 추천 (SSE 스트리밍)

```
- POST /api/ai/recommendations 는 로그인이 필요하다. 응답은 SSE(text/event-stream)다.
- 본문은 { messages: [{ role: 'user' | 'assistant', content }] } 이고, 맨 마지막이 새 질문이다.
  서버는 대화를 기억하지 않는다. 화면이 대화 기록을 들고 있다가 요청마다 함께 보낸다.
  최근 10개(api/aiApi.js 의 MAX_HISTORY)만 보낸다. 서버도 그 이상은 버리고, 30개를 넘으면 400 이다.
- content 는 500자 이하다.
- 이벤트는 filter -> candidates -> delta(여러 번) -> recommended -> done 순서로 온다. data 는 모두 JSON 이다.
    filter      : { categoryName, maxPrice, excludeAllergens[], keywords[] }  AI 가 뽑은 검색 조건
    candidates  : { count }                                                    조건에 맞는 후보 메뉴 개수
    delta       : { text }                                                     응답의 글자 조각. 이어 붙여 보여준다
    recommended : { menus: [MenuResponse] }                                    응답에 이름이 나온 메뉴 (카드로 보여준다)
    done        : {}
    error       : { message }                                                  도중에 실패한 경우. 이 이벤트로 끝난다
- SSE 를 열기 전의 실패(400 검증 실패, 401, 503 서버에 OPENAI_API_KEY 없음)는 평소의 JSON 오류 응답으로 온다.
- 이 요청은 api/aiApi.js 의 streamRecommendation() 하나로만 보낸다. 컴포넌트에서 fetch 를 직접 쓰지 않는다.
- 응답을 기다리는 동안 [중단]을 누르면 AbortController 로 요청을 끊는다. 서버도 OpenAI 호출을 멈춘다.
- 호출할 때마다 OpenAI 요금이 나간다. 자동으로 반복 호출하거나 입력할 때마다 요청하는 화면을 만들지 않는다.
```

## 관리자 메뉴 설명 초안 (WebSocket)

```
- 주소는 ws://localhost:8080/ws/admin/menu-draft 다. 관리자 메뉴 등록 · 수정 화면에서만 쓴다.
- 연결은 api/menuDraftSocket.js 의 openMenuDraftSocket() 하나로만 연다. 컴포넌트에서 new WebSocket 을 직접 쓰지 않는다.
- 메시지는 모두 JSON 이고 type 으로 구분한다.
    보내기 : { type: 'auth', token }                       연결 직후 첫 메시지. 반드시 먼저 보낸다
             { type: 'draft', menuName, categoryName, menuPrice, menuIngredients }
             { type: 'revise', instruction }                 직전 초안을 고친다. 200자 이하
             { type: 'cancel' }                              만들고 있는 것을 멈춘다
             { type: 'image', menuName, categoryName, menuIngredients, menuDescription }   메뉴 사진 (선택 기능)
    받기   : auth_ok                                         인증 성공. 이 뒤에 draft · image 를 보낸다
             delta { text }                                  초안 글자 조각. 이어 붙여 설명 칸에 넣는다
             done { text }                                   초안 완성 (전체 글)
             image_progress { data } / image { data }        사진 미리보기 / 완성본. data 는 'data:image/jpeg;base64,...'
             cancelled                                       멈춤
             error { message }                               실패. 연결은 열려 있다 (인증 실패만 연결이 닫힌다)
- 인증에 실패하면(토큰 없음 · 만료 · 관리자 아님) 서버가 error 를 보낸 뒤 1008 코드로 연결을 닫는다.
- 서버는 이 연결의 초안 대화를 기억한다. 다시 쓰기는 고칠 점 한 줄만 보낸다. 연결이 닫히면 대화도 사라진다.
- 한 연결에서는 한 번에 하나만 만든다. 만드는 중에 또 보내면 error 가 온다.
- 화면을 떠날 때(컴포넌트가 사라질 때) 연결을 닫는다. 서버도 만들던 것을 멈춘다.
- 완성된 사진(data URL)은 File 로 바꿔 메뉴 이미지 입력과 같은 자리에 넣는다. 저장은 기존 메뉴 등록 · 수정 요청이 한다.
- 이 도우미는 메뉴 등록 <form> 안에 들어간다. 안에 <form> 을 또 만들지 않는다. (바깥 form 이 제출된다)
- 호출할 때마다 OpenAI 요금이 나간다. 사진 한 장은 초안 한 번보다 100배쯤 비싸다. 버튼을 누를 때만 보낸다.
```
