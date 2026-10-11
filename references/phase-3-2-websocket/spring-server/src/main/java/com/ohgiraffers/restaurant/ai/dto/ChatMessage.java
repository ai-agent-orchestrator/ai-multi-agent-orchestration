package com.ohgiraffers.restaurant.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* 설명. 손님과 AI 가 주고받은 메시지 하나를 담는 DTO
 *  - role    : 메시지 작성자. 'user'(손님) 또는 'assistant'(AI)
 *  - content : 메시지 내용
 *  ----------------------------------------------------------------------------------
 *  이 DTO 가 쓰이는 곳
 *  1) React -> Spring  : AI 추천 요청 본문(RecommendationRequest)의 messages 목록. 지금까지 주고받은 대화를 담는다.
 *  2) Spring -> OpenAI : OpenAiClient 가 이 목록의 role · content 를 그대로 옮겨 요청 본문의 input 에 넣는다.
 *     그래서 필드 이름을 OpenAI Responses API 의 input 메시지({ "role": ..., "content": ... })와 같게 맞췄다.
 *     공식 문서 : https://developers.openai.com/api/docs/guides/conversation-state
 *  ----------------------------------------------------------------------------------
 *  추천 채팅의 대화 기록은 서버가 저장하지 않고, React측에서 state로 관리하여 요청마다 최근 대화를 함께 보낸다.
 * */
public class ChatMessage {

    @NotBlank(message = "role 은 필수입니다.")
    @Pattern(regexp = "user|assistant", message = "role 은 user 또는 assistant 입니다.")
    private String role;

    // 메시지가 너무 길면 그만큼 입력 토큰(비용)이 늘어나므로 길이를 제한한다.
    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 500, message = "메시지는 500자 이하로 입력해주세요.")
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
