package com.ohgiraffers.restaurant.ai.service;

import com.ohgiraffers.restaurant.ai.client.OpenAiClient;
import com.ohgiraffers.restaurant.ai.dto.ChatMessage;
import com.ohgiraffers.restaurant.ai.dto.MenuDraftMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* 설명. [Phase 3-2] 관리자용 메뉴 설명 초안
 *  ----------------------------------------------------------------------------------
 *  메뉴 이름, 카테고리, 가격, 재료를 보고 메뉴 상세설명(tbl_menu.menu_description)의 초안을 쓴다.
 *  답은 스트리밍으로 받고, 글자 조각마다 onDelta 를 호출해 WebSocket 으로 바로 보낸다. (MenuDraftWebSocketHandler 참고)
 *  ----------------------------------------------------------------------------------
 *  대화 기록(history)을 서버가 들고 있다는 점이 3-1 의 추천 채팅과 다르다.
 *  - 3-1 (SSE)       : 요청 하나가 끝나면 연결도 끝난다. 그래서 React 가 대화를 들고 있다가 매번 다시 보낸다.
 *  - 3-2 (WebSocket) : 연결이 계속 열려 있다. 연결마다 대화를 서버에 두면, '더 짧게'만 보내도 무엇을 고칠지 안다.
 *  history 는 연결(WebSocket 세션)마다 하나씩 따로 만들어 핸들러가 보관한다.
 *  ----------------------------------------------------------------------------------
 *  이 서비스는 핸들러와 OpenAiClient 사이에 있다. 핸들러가 넘긴 onDelta(조각마다 할 일)를 OpenAiClient 까지 전달한다.
 *  주석의 [Client 호출] 은 OpenAiClient 로 넘어가는 곳, [보조 메서드] 는 이 클래스 안의 메서드를 호출하는 곳이다.
 * */
@Service
public class MenuDraftService {

    // 서버에 들고 있을 최근 대화 개수 (초안을 여러 번 고쳐도 입력 토큰이 끝없이 늘지 않게)
    private static final int MAX_MESSAGES = 8;

    // AI 에게 줄 지시문(시스템 프롬프트). 기존 메뉴 설명과 문체, 길이가 같아지도록 예시 문장을 넣었다.
    private static final String INSTRUCTIONS = """
            너는 '오지라퍼 레스토랑'의 메뉴판 문구를 쓰는 직원이다. 메뉴 정보를 보고 메뉴 상세설명을 쓴다.
            - 2~3문장, 200자 이내로 쓴다.
            - 문장은 '~다.'로 끝낸다. (예: 에스프레소에 열무김치 국물을 더한 라떼다.)
            - 재료에 없는 재료를 지어내지 않는다. 건강 효능을 과장하지 않는다.
            - 맛 · 식감 · 어울리는 상황 중 손님이 고르는 데 도움이 되는 것을 담는다.
            - 수정을 부탁받으면 직전 설명을 그 요청대로 고쳐 쓴다.
            - 설명 문장만 답한다. 인사말, 따옴표, 마크다운 기호를 붙이지 않는다.
            """;

    // OpenAI 를 호출하는 부품 (3-1 과 같은 것)
    private final OpenAiClient openAiClient;
    // application.yaml 의 openai.*.draft 값
    private final String model;
    private final int maxOutputTokens;
    private final String reasoningEffort;

    public MenuDraftService(OpenAiClient openAiClient,
                            @Value("${openai.model.draft}") String model,
                            @Value("${openai.max-output-tokens.draft}") int maxOutputTokens,
                            @Value("${openai.reasoning-effort.draft:}") String reasoningEffort) {
        this.openAiClient = openAiClient;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.reasoningEffort = reasoningEffort;
    }

    /* 설명. API 키가 설정되어 있는지 확인 용도. 핸들러는 OpenAiClient 를 직접 모르므로 이 서비스를 통해 물어본다. */
    public boolean isEnabled() {
        // [Client 호출] isEnabled() : 키가 있는지만 확인한다. (OpenAI 서버에 요청을 보내지는 않는다)
        return openAiClient.isEnabled();
    }

    /* 목차. 1. 첫 초안 */
    /**
     * 메뉴 정보로 첫 초안을 쓴다. 이전 대화는 지우고 새로 시작한다.
     *
     * @param history 이 연결의 대화 기록 (이 메서드가 채운다)
     * @return 완성된 초안 전체
     */
    public String draft(List<ChatMessage> history, MenuDraftMessage menu, Consumer<String> onDelta) {

        // [AI 초안 쓰기]를 다시 누르면 새 메뉴 정보로 처음부터 시작한다
        history.clear();
        // 메뉴 정보를 대화의 첫 번째 user 메시지로 넣는다. (오래된 대화를 버릴 때도 이 첫 메시지는 남긴다, generate 참고)
        history.add(new ChatMessage("user", """
                다음 메뉴의 상세설명을 써 줘.
                - 메뉴 이름 : %s
                - 카테고리 : %s
                - 가격 : %s
                - 재료 : %s""".formatted(
                menu.getMenuName(),
                // [보조 메서드] orNone() : 비어 있는 값은 '정하지 않음'으로 적는다
                orNone(menu.getCategoryName()),
                menu.getMenuPrice() == null ? "정하지 않음" : menu.getMenuPrice() + "원",
                orNone(menu.getMenuIngredients()))));

        System.out.println("[MenuDraftService] 초안 요청 : " + menu.getMenuName());
        // [보조 메서드] generateOrRollback() : OpenAI 에 보내 초안을 받는다. 실패하면 방금 넣은 메시지를 되돌린다.
        return generateOrRollback(history, onDelta);
    }

    /* 목차. 2. 고쳐 쓰기 */
    /**
     * 직전 초안을 지시대로 고친다. 서버가 대화를 들고 있으므로 지시 한 줄만 받으면 된다.
     */
    public String revise(List<ChatMessage> history, String instruction, Consumer<String> onDelta) {

        // 고칠 점 한 줄을 대화 끝에 더한다. 앞의 메뉴 정보와 직전 초안은 이미 history 에 있다.
        history.add(new ChatMessage("user", instruction));

        System.out.println("[MenuDraftService] 고쳐 쓰기 요청 : " + instruction + " (대화 " + history.size() + "개)");
        // [보조 메서드] generateOrRollback() : 대화 전체를 보내 고친 초안을 받는다
        return generateOrRollback(history, onDelta);
    }

    /* 설명. 중간에 멈추거나 실패하면 방금 넣은 요청을 대화 기록에서 뺀다.
     *  답이 없는 요청이 남아 있으면, 다음 고쳐 쓰기 때 AI 가 무엇을 고쳐야 할지 헷갈린다.
     * */
    private String generateOrRollback(List<ChatMessage> history, Consumer<String> onDelta) {
        try {
            // [보조 메서드] generate() : 실제로 OpenAI 를 호출한다
            return generate(history, onDelta);
        } catch (RuntimeException e) {
            // [중단]이나 실패로 끝났다. 방금 넣은 요청(마지막 항목)을 뺀다
            history.remove(history.size() - 1);
            // 예외는 그대로 다시 던져 핸들러가 cancelled 또는 error 메시지를 보내게 한다
            throw e;
        }
    }

    /* 설명. 대화 기록을 보내고 답을 스트리밍으로 받는다. 다 받으면 답도 대화 기록에 넣는다. */
    private String generate(List<ChatMessage> history, Consumer<String> onDelta) {

        // 오래된 대화는 버린다. 단, 맨 처음의 메뉴 정보는 남겨 둬야 무엇에 대한 설명인지 잊지 않는다.
        while (history.size() > MAX_MESSAGES) {
            history.remove(1);
        }

        // 조각을 이어 붙여 완성된 초안을 모아 둘 곳
        StringBuilder text = new StringBuilder();
        // [Client 호출] streamText() : 지시문과 대화 기록을 보내고, 응답을 글자 조각으로 나눠 받는다.
        //  - 모델, 추론 정도, 출력 상한은 application.yaml 의 openai.*.draft 값이다.
        //  - history 는 복사본을 넘긴다. 원본 대화 기록은 이 클래스에서만 바꾼다.
        //  - 마지막 인자(람다)는 조각이 올 때마다 OpenAiClient 가 호출한다.
        openAiClient.streamText(model, reasoningEffort, INSTRUCTIONS, new ArrayList<>(history), maxOutputTokens,
                delta -> {
                    // 완성본을 모으고
                    text.append(delta);
                    // 핸들러가 넘겨준 onDelta(= 핸들러의 sendDelta)를 호출해 화면으로 보낸다.
                    // [중단]을 받았으면 여기서 예외가 나고, OpenAI 응답 읽기도 함께 멈춘다.
                    onDelta.accept(delta);
                });

        // AI 의 초안도 대화 기록에 넣는다. 다음 고쳐 쓰기 때 '직전 초안'으로 쓰인다.
        history.add(new ChatMessage("assistant", text.toString()));
        // 완성된 초안 전체를 돌려준다. 핸들러가 done 메시지에 담아 보낸다.
        return text.toString();
    }

    /* 설명. 비어 있는 값은 '정하지 않음'으로 바꾼다. (관리자가 아직 입력하지 않은 칸) */
    private String orNone(String value) {
        return value == null || value.isBlank() ? "정하지 않음" : value;
    }
}
