package com.ohgiraffers.restaurant.ai.controller;

import com.ohgiraffers.restaurant.ai.dto.RecommendationRequest;
import com.ohgiraffers.restaurant.ai.service.RecommendationService;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI", description = "AI 메뉴 추천")
@RestController
@RequestMapping("/api/ai")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /* 목차. 1. AI 메뉴 추천 (SSE) */
    /* 설명. [Phase 3] 응답을 한 번에 돌려주지 않고, 만들어지는 대로 조금씩 흘려보낸다.
     *  ----------------------------------------------------------------------------------
     *  - produces = text/event-stream : 응답이 SSE(Server-Sent Events) 형식이라는 뜻이다.
     *  - 반환 타입 SseEmitter          : 이 메서드가 끝나도 응답은 끝나지 않는다. 서비스가 이벤트를 다 보내고
     *                                   complete() 를 부를 때 끝난다. (RecommendationService 참고)
     *  - POST 인 이유                  : 대화 내용을 본문(JSON)으로 보내야 하기 때문이다.
     *    브라우저의 EventSource 는 GET 만 되고 헤더(토큰)도 못 붙이므로, React 는 fetch 로 이 주소를 호출한다.
     *  ----------------------------------------------------------------------------------
     *  입력값 검증(@Valid) 실패와 키 미설정은 SSE 를 열기 전에 일어나므로 평소처럼 JSON 오류 응답(400, 503)이 나간다.
     *  SSE 가 열린 뒤의 실패는 error 이벤트로 알린다.
     * */
    @Operation(summary = "AI 메뉴 추천 (SSE 스트리밍)", description = """
            최근 대화를 보내면 조건에 맞는 메뉴를 추천한다. 응답은 SSE(text/event-stream)로 조금씩 온다.
            본문: { messages: [{ role('user'/'assistant'), content }] } (맨 마지막이 새 질문)
            이벤트 순서:
            - filter      : { categoryName, maxPrice, excludeAllergens[], keywords[] } AI 가 뽑은 검색 조건
            - candidates  : { count } 조건에 맞는 후보 메뉴 개수
            - delta       : { text } 추천 문장의 조각 (여러 번)
            - recommended : { menus: [MenuResponse] } 추천 문장에 이름이 나온 메뉴
            - done        : {} 끝
            - error       : { message } 도중에 실패한 경우
            - 400 : 입력값 검증 실패 / 401 : 토큰 없음·만료 / 503 : 서버에 OPENAI_API_KEY 가 없음
            """)
    @PostMapping(value = "/recommendations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter recommend(@Valid @RequestBody RecommendationRequest request,
                                @AuthenticationPrincipal CustomUserDetails user) {

        System.out.println("[RecommendationController] 추천 요청 : " + user.getMemberId());

        return recommendationService.recommend(request.getMessages(), user.getMemberId());
    }
}
