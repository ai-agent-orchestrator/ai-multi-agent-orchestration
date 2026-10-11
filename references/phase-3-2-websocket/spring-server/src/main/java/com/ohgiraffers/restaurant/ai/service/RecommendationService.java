package com.ohgiraffers.restaurant.ai.service;

import com.ohgiraffers.restaurant.ai.client.OpenAiClient;
import com.ohgiraffers.restaurant.ai.dto.ChatMessage;
import com.ohgiraffers.restaurant.ai.dto.MenuFilter;
import com.ohgiraffers.restaurant.common.exception.AiUnavailableException;
import com.ohgiraffers.restaurant.menu.domain.Allergen;
import com.ohgiraffers.restaurant.menu.domain.AllergenRepository;
import com.ohgiraffers.restaurant.menu.domain.Category;
import com.ohgiraffers.restaurant.menu.domain.CategoryRepository;
import com.ohgiraffers.restaurant.menu.domain.Menu;
import com.ohgiraffers.restaurant.menu.domain.MenuRepository;
import com.ohgiraffers.restaurant.menu.dto.MenuResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/* 설명. [Phase 3] AI 메뉴 추천
 *  ----------------------------------------------------------------------------------
 *  손님이 보낸 질문 하나를 세 단계로 처리하고, 단계마다 결과를 SSE 이벤트로 바로 브라우저에 보낸다.
 *  1) 조건 뽑기  : OpenAI 구조화 출력으로 손님 말에서 카테고리, 가격, 빼야 할 알레르기를 JSON 으로 뽑는다  -> event: filter
 *  2) 후보 고르기 : 그 조건으로 DB 에서 주문 가능한 메뉴를 고른다 (AI 가 아니라 JPA 가 한다)               -> event: candidates
 *  3) 추천하기    : 후보 메뉴만 보여 주고 그 안에서 추천하게 한다. 응답을 스트리밍으로 받아 조각마다 보낸다 -> event: delta (여러 번)
 *                  다 받으면 응답에 이름이 나온 메뉴를 카드로 보여 줄 수 있게 보낸다                     -> event: recommended
 *                  마지막으로 끝났다고 알린다                                                           -> event: done
 *  실패하면 그 자리에서 이유를 보낸다                                                                   -> event: error
 *  ----------------------------------------------------------------------------------
 *  AI 에게 DB 를 통째로 주지 않고 1) -> 2) 로 후보를 줄이는 이유
 *  - AI 는 없는 메뉴를 그럴듯하게 지어낼 수 있다. 실제 메뉴 목록 안에서만 고르게 해야 한다.
 *  - 가격, 알레르기 같은 조건은 AI 의 판단보다 DB 쿼리가 정확하다. (알레르기는 틀리면 안 된다)
 *  - 프롬프트에 넣는 메뉴가 적을수록 입력 토큰(비용)이 줄어든다.
 *  ----------------------------------------------------------------------------------
 *  OpenAI 서버와 실제로 통신하는 것은 OpenAiClient 다. 이 클래스는 '언제, 무엇을 물을지'만 정한다.
 *  주석의 [Client 호출] 은 OpenAiClient 로 넘어가는 곳, [보조 메서드] 는 이 클래스 안의 메서드를 호출하는 곳이다.
 * */
@Service
public class RecommendationService {

    // OpenAI 에 보낼 최근 대화 개수 (질문과 응답을 합쳐 10개 = 5번 주고받기). 오래된 대화는 잘라서 비용을 줄인다.
    private static final int MAX_MESSAGES = 10;
    // 프롬프트에 넣을 후보 메뉴의 최대 개수
    private static final int MAX_CANDIDATES = 30;
    // SSE 연결을 유지하는 최대 시간(밀리초). 이 시간이 지나면 연결이 끊긴다.
    private static final long SSE_TIMEOUT = 60_000L;

    // OpenAI 를 호출하는 부품. 요청을 조립하고 응답을 해석하는 일은 모두 이 부품이 맡는다.
    private final OpenAiClient openAiClient;
    // 2-2 에서 후보 메뉴를 고를 때 쓴다.
    private final MenuRepository menuRepository;
    // 카테고리, 알레르기 이름 목록을 읽어 구조화 출력 스키마의 enum 을 만들 때 쓴다. (extractFilter 참고)
    private final CategoryRepository categoryRepository;
    private final AllergenRepository allergenRepository;
    // OpenAI 가 돌려준 JSON(JsonNode)을 MenuFilter 객체로 바꿀 때 쓴다.
    private final ObjectMapper objectMapper;
    // 추천 작업을 실행할 스레드 풀. 요청 스레드는 SseEmitter 를 돌려주고 바로 끝난다. (아래 recommend 참고)
    private final TaskExecutor taskExecutor;

    // application.yaml 의 openai.* 값. 조건 뽑기(extract)와 추천하기(chat)에 따로 정해 둔다.
    private final String extractModel;
    private final String chatModel;
    private final int extractMaxTokens;
    private final int chatMaxTokens;
    private final String extractEffort;
    private final String chatEffort;

    public RecommendationService(OpenAiClient openAiClient,
                                 MenuRepository menuRepository,
                                 CategoryRepository categoryRepository,
                                 AllergenRepository allergenRepository,
                                 ObjectMapper objectMapper,
                                 // Spring Boot 가 만들어 둔 비동기 작업용 실행기. 가상 스레드를 켜면 가상 스레드로 실행한다.
                                 @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                                 @Value("${openai.model.extract}") String extractModel,
                                 @Value("${openai.model.chat}") String chatModel,
                                 @Value("${openai.max-output-tokens.extract}") int extractMaxTokens,
                                 @Value("${openai.max-output-tokens.chat}") int chatMaxTokens,
                                 @Value("${openai.reasoning-effort.extract:}") String extractEffort,
                                 @Value("${openai.reasoning-effort.chat:}") String chatEffort) {
        this.openAiClient = openAiClient;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.allergenRepository = allergenRepository;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.extractModel = extractModel;
        this.chatModel = chatModel;
        this.extractMaxTokens = extractMaxTokens;
        this.chatMaxTokens = chatMaxTokens;
        this.extractEffort = extractEffort;
        this.chatEffort = chatEffort;
    }

    /* 목차. 1. 추천 시작 (요청 스레드에서 실행) */
    /* 설명. SseEmitter 는 '응답 통로'다. 컨트롤러가 이것을 돌려주면 HTTP 응답이 끝나지 않고 열린 채로 남는다.
     *  ----------------------------------------------------------------------------------
     *  실제 작업(OpenAI 호출, DB 조회)은 taskExecutor 의 다른 스레드에 맡기고, 요청 스레드는 바로 돌아간다.
     *  다른 스레드가 emitter.send() 로 이벤트를 보낼 때마다 브라우저에 한 덩어리씩 도착하고,
     *  emitter.complete() 를 호출하면 응답이 끝난다.
     * */
    public SseEmitter recommend(List<ChatMessage> messages, String memberId) {

        // [Client 호출] isEnabled() : API 키가 설정되어 있는지만 확인한다. (OpenAI 서버에 요청을 보내지는 않는다)
        // 키가 없으면 SSE 를 열기 전에 막는다. 그래야 일반 오류 응답(JSON, 503)으로 나간다.
        if (!openAiClient.isEnabled()) {
            throw new AiUnavailableException("서버에 OPENAI_API_KEY 가 설정되지 않아 AI 추천을 쓸 수 없습니다.");
        }

        // [보조 메서드] recentMessages() : 대화가 길면 최근 10개만 남긴다. OpenAI 에 보낼 입력 토큰(비용)을 줄이기 위해서다.
        List<ChatMessage> recentMessages = recentMessages(messages);
        // 응답 통로를 만든다. 60초 안에 complete() 하지 않으면 시간 초과로 끊긴다.
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        /* 설명. 브라우저가 연결을 끊었는지 기억해 두는 깃발
         *  손님이 [중단]을 누르거나 창을 닫으면, 남은 조각을 더 받지 않고 OpenAI 호출도 끊어 비용을 아낀다.
         *  여러 스레드가 함께 보는 값이라 AtomicBoolean 을 쓴다.
         * */
        AtomicBoolean closed = new AtomicBoolean(false);
        // 응답이 정상으로 끝났을 때 (complete)
        emitter.onCompletion(() -> closed.set(true));
        // 60초가 지났을 때
        emitter.onTimeout(() -> closed.set(true));
        // 브라우저가 연결을 끊는 등 오류가 났을 때
        emitter.onError(e -> closed.set(true));

        System.out.println("[RecommendationService] 추천 시작 : " + memberId
                + ", 받은 메시지 " + messages.size() + "개 중 최근 " + recentMessages.size() + "개 사용"
                + " (요청 스레드 : " + Thread.currentThread() + ")");

        // 실제 작업(run)을 작업 스레드에 넘긴다. 이 줄부터 run() 은 다른 스레드에서 따로 실행된다.
        taskExecutor.execute(() -> run(emitter, recentMessages, closed));

        // 요청 스레드는 응답 통로만 돌려주고 여기서 끝난다. Spring 이 응답을 열어 둔 채 요청 스레드를 풀로 돌려보낸다.
        return emitter;
    }

    /* 목차. 2. 추천 실행 (taskExecutor 의 스레드에서 실행) */
    private void run(SseEmitter emitter, List<ChatMessage> messages, AtomicBoolean closed) {

        // 가상 스레드를 켰다면 VirtualThread[#..] 로 찍힌다.
        System.out.println("[RecommendationService] 작업 스레드 : " + Thread.currentThread());

        try {
            // 2-1. 조건 뽑기
            // [보조 메서드] extractFilter() : 손님과의 대화를 OpenAI 에 보내 검색 조건(MenuFilter)으로 바꾼다. (OpenAI 1번째 호출)
            MenuFilter filter = extractFilter(messages);
            // [보조 메서드] send() : 뽑은 조건을 브라우저에 보낸다. 화면은 이것으로 조건 칩을 그린다.  -> event: filter
            send(emitter, closed, "filter", filter);

            // 2-2. 후보 고르기
            // 뽑은 조건으로 DB 에서 후보 메뉴를 고른다. AI 가 아니라 쿼리가 고르므로 가격, 알레르기 조건이 정확하다.
            // 키워드(맛, 온도 등)는 쿼리로 거르기 어려워 쓰지 않는다. 키워드는 2-3 의 지시문에 넣어 AI 가 판단하게 한다.
            List<Menu> candidates = menuRepository.findRecommendationCandidates(
                    filter.getCategoryName(), filter.getMaxPrice(), filter.getExcludeAllergens(),
                    // 첫 페이지(0)에서 최대 30개만 가져온다 (프롬프트가 길어지면 비용이 늘어난다)
                    PageRequest.of(0, MAX_CANDIDATES));
            System.out.println("[RecommendationService] 후보 메뉴 " + candidates.size() + "개");
            // 후보가 몇 개인지 알린다.  -> event: candidates
            send(emitter, closed, "candidates", Map.of("count", candidates.size()));

            // 2-3. 추천하기 (조각마다 바로 보내고, 다 모은 응답은 2-4 에서 쓴다)
            // 글자 조각을 이어 붙여 둘 곳. 2-4 에서 응답 전체를 보고 카드로 보여 줄 메뉴를 고른다.
            StringBuilder answer = new StringBuilder();
            // [Client 호출] streamText() : 지시문과 대화를 보내고, 응답을 글자 조각으로 나눠 받는다. (OpenAI 2번째 호출)
            //  - 모델, 추론 정도, 출력 상한은 application.yaml 의 openai.*.chat 값이다.
            //  - [보조 메서드] recommendInstructions() 가 [손님 조건]과 [후보 메뉴]를 넣은 지시문을 만든다.
            //  - 마지막 인자(람다)는 '조각이 올 때마다 할 일'이다. streamText() 가 조각을 받을 때마다 이 람다를 호출한다.
            //    OpenAI 에 보내는 요청은 1번이고, 람다는 조각 수만큼 여러 번 호출된다.
            openAiClient.streamText(chatModel, chatEffort, recommendInstructions(filter, candidates), messages, chatMaxTokens,
                    delta -> {
                        // 응답 전체를 모아 둔다 (2-4 에서 쓴다)
                        answer.append(delta);
                        // 조각을 바로 브라우저에 보낸다. 화면은 받은 글자를 이어 붙인다.  -> event: delta
                        // 브라우저가 연결을 끊었으면 여기서 예외가 나고, streamText() 도 OpenAI 응답 읽기를 멈춘다.
                        send(emitter, closed, "delta", Map.of("text", delta));
                    });

            // 2-4. 응답에 이름이 나온 메뉴만 카드로 보여 줄 수 있게 보낸다.
            //      글자로 찾는 간단한 방법이라, AI 가 '이 메뉴는 빼세요'라고 이름을 말해도 카드에 나온다.
            //      그래서 지시문에 '추천하지 않는 메뉴는 이름을 말하지 않는다'는 규칙을 넣었다.
            List<MenuResponse> recommended = candidates.stream()
                    // 응답 글자 안에 메뉴 이름이 들어 있는 후보만 남기고
                    .filter(menu -> answer.indexOf(menu.getMenuName()) >= 0)
                    // 메뉴 조회 API 와 같은 응답 DTO 로 바꾼다
                    .map(MenuResponse::from)
                    .toList();
            // 화면은 이것으로 추천 카드를 그린다.  -> event: recommended
            send(emitter, closed, "recommended", Map.of("menus", recommended));

            // 모두 보냈다고 알린다.  -> event: done
            send(emitter, closed, "done", Map.of());
            // 응답 통로를 닫는다. 이때 HTTP 응답이 끝난다.
            emitter.complete();
            System.out.println("[RecommendationService] 추천 완료 : 추천 메뉴 " + recommended.size() + "개");

        } catch (ClientClosedException e) {
            // 브라우저가 먼저 연결을 끊었다. 보낼 곳이 없으므로 로그만 남기고 끝낸다.
            System.out.println("[RecommendationService] 브라우저가 연결을 끊어 추천을 멈춘다");

        } catch (RuntimeException e) {
            // 실패 이유를 error 이벤트로 알리고 응답을 끝낸다. (이미 응답 중이라 상태 코드는 바꿀 수 없다)
            System.out.println("[RecommendationService] 실패 : " + e.getMessage());
            try {
                // 화면은 이 문장을 그대로 보여 준다.  -> event: error
                send(emitter, closed, "error", Map.of("message", e.getMessage()));
                // 응답 통로를 닫는다.
                emitter.complete();
            } catch (ClientClosedException ignored) {
                // 오류를 알리려는 사이에 브라우저가 끊었다.
            }
        }
    }

    /* 목차. 3. 조건 뽑기 (구조화 출력) */
    private MenuFilter extractFilter(List<ChatMessage> messages) {

        // DB 에 실제로 있는 카테고리, 알레르기 이름만 고를 수 있게, 이름 목록을 스키마의 enum 으로 넣는다.
        // (서버 로그에서 OpenAI 호출 전에 tbl_category, tbl_allergen 조회 쿼리가 찍히는 이유다)
        List<String> categoryNames = categoryRepository.findAll().stream().map(Category::getCategoryName).toList();
        List<String> allergenNames = allergenRepository.findAll().stream().map(Allergen::getAllergenName).toList();

        // AI 에게 줄 지시문(시스템 프롬프트). 대화와 따로 요청 본문의 instructions 필드에 들어간다.
        String instructions = """
                너는 식당 주문 도우미다. 손님과의 대화에서 메뉴 검색 조건만 뽑는다.
                - 손님이 말하지 않은 조건은 null 또는 빈 배열로 둔다. 짐작해서 채우지 않는다.
                - 대화가 여러 번 오갔다면 가장 최근에 한 말을 우선한다.
                - '만원 이하'는 maxPrice 10000 처럼 원 단위 숫자로 바꾼다.
                - 알레르기는 주어진 이름 중에서만 고른다. (예: '견과류'는 땅콩 · 호두 · 잣)
                - 음식 종류를 말하면(식사, 밥, 음료, 마실 것, 커피, 디저트, 후식 등) 가장 알맞은 categoryName 을 고른다.
                  카테고리 이름과 같은 말은 keywords 에 넣지 않는다.
                - 카테고리 · 가격 · 알레르기로 표현할 수 없는 바람(맛, 온도, 분위기)은 keywords 에 짧게 넣는다.
                """;

        // [Client 호출] createJson() : 지시문, 대화, 스키마를 보내고, 스키마 모양의 JSON 을 한 번에 받는다. (OpenAI 1번째 호출)
        //  - 모델, 추론 정도, 출력 상한은 application.yaml 의 openai.*.extract 값이다.
        //  - "menu_filter" 는 스키마 이름이고, [보조 메서드] filterSchema() 가 스키마 본문(MenuFilter 와 같은 모양)을 만든다.
        JsonNode json = openAiClient.createJson(extractModel, extractEffort, instructions, messages, extractMaxTokens,
                "menu_filter", filterSchema(categoryNames, allergenNames));

        // 받은 JSON 을 MenuFilter 객체로 바꾼다. 스키마의 필드 이름을 MenuFilter 의 필드 이름과 같게 만들었으므로 그대로 들어간다.
        MenuFilter filter = objectMapper.treeToValue(json, MenuFilter.class);
        System.out.println("[RecommendationService] 뽑은 조건 : " + filter);

        return filter;
    }

    /* 설명. 구조화 출력에 쓰는 JSON Schema (MenuFilter 와 같은 모양)
     *  strict 모드의 규칙 : 모든 필드를 required 에 넣고, additionalProperties 는 false.
     *  '없을 수도 있는 값'은 required 에서 빼는 대신 type 에 "null" 을 함께 적어 표현한다.
     * */
    private Map<String, Object> filterSchema(List<String> categoryNames, List<String> allergenNames) {

        // categoryName 에 들어갈 수 있는 값 : DB 의 카테고리 이름들 + null (손님이 카테고리를 말하지 않은 경우)
        // List.of() 는 null 을 넣을 수 없어서 ArrayList 로 만든다.
        List<Object> categoryEnum = new ArrayList<>(categoryNames);
        categoryEnum.add(null);

        // 필드 이름 -> 필드 규칙. 넣은 순서를 지키는 LinkedHashMap 을 쓴다.
        Map<String, Object> properties = new LinkedHashMap<>();
        // 카테고리 : 문자열 또는 null, 값은 enum 목록 중 하나
        properties.put("categoryName", Map.of("type", List.of("string", "null"), "enum", categoryEnum));
        // 최대 가격 : 정수 또는 null (strict 규칙 3 : 없을 수도 있는 값은 type 에 null 을 함께 적는다)
        properties.put("maxPrice", Map.of("type", List.of("integer", "null")));
        // 뺄 알레르기 : 문자열 배열, 각 값은 DB 의 알레르기 이름 중 하나
        properties.put("excludeAllergens", Map.of("type", "array", "items", Map.of("type", "string", "enum", allergenNames)));
        // 그 밖의 바람(맛, 온도, 분위기) : 자유로운 문자열 배열
        properties.put("keywords", Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> schema = new LinkedHashMap<>();
        // 응답 전체는 JSON 객체다 (strict 규칙 4 : 맨 바깥은 object)
        schema.put("type", "object");
        // 위에서 정한 네 필드
        schema.put("properties", properties);
        // strict 규칙 1 : 모든 필드를 required 에 넣는다
        schema.put("required", List.of("categoryName", "maxPrice", "excludeAllergens", "keywords"));
        // strict 규칙 2 : 정하지 않은 필드는 만들지 못한다
        schema.put("additionalProperties", false);

        return schema;
    }

    /* 목차. 4. 추천 지시문 (후보 메뉴를 넣은 프롬프트) */
    private String recommendInstructions(MenuFilter filter, List<Menu> candidates) {

        // 후보 메뉴를 한 줄에 하나씩 '이름 | 가격 | 카테고리 | 재료 | 설명' 모양으로 적는다.
        // 재료를 함께 주어야 AI 가 알레르기, 맛을 근거로 추천 이유를 설명할 수 있다.
        StringBuilder menuList = new StringBuilder();

        for (Menu menu : candidates) {
            menuList.append("- ").append(menu.getMenuName())
                    .append(" | ").append(menu.getMenuPrice()).append("원")
                    // 카테고리는 쿼리에서 JOIN FETCH 로 함께 가져왔으므로 작업 스레드에서도 읽을 수 있다. (MenuRepository 참고)
                    .append(" | ").append(menu.getCategory().getCategoryName())
                    .append(" | 재료: ").append(menu.getMenuIngredients())
                    .append(" | 설명: ").append(menu.getMenuDescription())
                    .append('\n');
        }

        // 역할 -> 규칙 -> 자료(손님 조건, 후보 메뉴) 순서로 지시문을 만든다.
        // %s, %d, %s 자리에 describe() 의 결과, 후보 개수, 위에서 만든 메뉴 목록이 차례로 들어간다.
        return """
                너는 '오지라퍼 레스토랑'의 메뉴 추천 직원이다. 손님에게 친근하게 말한다.
                - 반드시 아래 [후보 메뉴]에 있는 메뉴만 추천한다. 목록에 없는 메뉴를 지어내지 않는다.
                - 메뉴 이름은 목록에 적힌 그대로 쓴다.
                - 추천하지 않는 메뉴는 이름을 말하지 않는다. (답에 이름이 나온 메뉴는 화면에 추천 카드로 보여 준다)
                - 1~3개를 골라 각각 한두 문장으로 이유를 말한다. 가격은 원 단위로 적는다.
                - 후보가 없으면 조건을 조금 바꿔 보라고 안내한다.
                - 마크다운 기호(**, # 등) 없이 평문으로, 전체 6문장 이내로 답한다.
                - 손님이 메뉴와 상관없는 이야기를 하면 메뉴 추천을 도와줄 수 있다고 짧게 안내한다.

                [손님 조건] %s
                [후보 메뉴] %d개
                %s""".formatted(describe(filter), candidates.size(), menuList);
    }

    /* 설명. 뽑은 조건(MenuFilter)을 AI 가 읽기 쉬운 한 줄로 바꾼다.
     *  예) 카테고리=커피, 최대 가격=상관없음, 뺄 알레르기=[], 바람=[추운 새벽]
     *  쿼리에 쓰지 않은 keywords(바람)도 여기에 넣어, 후보 중에서 고를 때 AI 가 참고하게 한다.
     * */
    private String describe(MenuFilter filter) {
        // null 은 '손님이 말하지 않은 조건'이므로 '상관없음'으로 적는다
        return "카테고리=" + (filter.getCategoryName() == null ? "상관없음" : filter.getCategoryName())
                + ", 최대 가격=" + (filter.getMaxPrice() == null ? "상관없음" : filter.getMaxPrice() + "원")
                + ", 뺄 알레르기=" + filter.getExcludeAllergens()
                + ", 바람=" + filter.getKeywords();
    }

    /* 설명. 최근 대화만 남긴다. (슬라이딩 윈도우)
     *  대화가 길어질수록 매번 보내는 입력 토큰이 늘어나므로, 오래된 대화는 잘라서 비용을 일정하게 유지한다.
     * */
    private List<ChatMessage> recentMessages(List<ChatMessage> messages) {
        // 뒤에서부터 MAX_MESSAGES 개를 남긴다. 대화가 그보다 짧으면 0 부터, 즉 전부 남긴다.
        int from = Math.max(0, messages.size() - MAX_MESSAGES);
        return messages.subList(from, messages.size());
    }

    /* 설명. SSE 이벤트 하나를 보낸다.
     *      event: delta
     *      data: {"text":"오늘은"}
     *  data 를 JSON 으로 보내는 이유 : 글자 조각에 줄바꿈이 들어 있어도 이벤트 모양이 깨지지 않는다.
     *  브라우저가 이미 연결을 끊었으면 send() 가 IOException 을 던진다 -> ClientClosedException 으로 바꿔 작업을 멈춘다.
     * */
    private void send(SseEmitter emitter, AtomicBoolean closed, String event, Object data) {

        // 이미 끊긴 것을 알고 있으면 보내지 않고 바로 멈춘다. (recommend() 에서 등록한 onCompletion, onTimeout, onError 가 세운 깃발)
        if (closed.get()) {
            throw new ClientClosedException();
        }

        try {
            // event: 이름 / data: 객체를 JSON 으로 바꾼 것. 보내는 즉시 브라우저에 도착한다.
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException e) {
            // 이미 끊긴 연결에 보내면 실패한다. 끊겼다고 기록하고
            closed.set(true);
            // 예외로 작업(run)을 멈춘다. streamText() 의 람다 안에서 났다면 OpenAI 응답 읽기도 함께 멈춘다.
            throw new ClientClosedException();
        }
    }

    /* 설명. 브라우저가 연결을 끊었음을 알리는 내부용 예외 */
    private static class ClientClosedException extends UncheckedIOException {
        ClientClosedException() {
            super(new IOException("브라우저가 연결을 끊었다"));
        }
    }
}
