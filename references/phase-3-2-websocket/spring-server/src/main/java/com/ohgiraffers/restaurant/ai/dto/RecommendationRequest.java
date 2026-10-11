package com.ohgiraffers.restaurant.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/* 설명. AI 메뉴 추천 요청 본문
 *  messages : 지금까지의 대화. 맨 마지막이 손님의 새 질문이다.
 *  서버는 이 중 최근 몇 개만 OpenAI 에 보낸다. (RecommendationService.MAX_MESSAGES)
 * */
public class RecommendationRequest {

    // List<@Valid ChatMessage> : 목록 안의 ChatMessage 하나하나에 붙은 검증(@NotBlank 등)도 함께 실행한다.
    @NotEmpty(message = "메시지가 하나 이상 있어야 합니다.")
    @Size(max = 30, message = "대화가 너무 깁니다. 새 대화를 시작해주세요.")
    private List<@Valid ChatMessage> messages;

    public RecommendationRequest() {}

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
}
