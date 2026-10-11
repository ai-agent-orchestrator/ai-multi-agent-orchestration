package com.ohgiraffers.restaurant.ai.client;

import com.ohgiraffers.restaurant.ai.dto.ChatMessage;
import com.ohgiraffers.restaurant.common.exception.AiRequestException;
import com.ohgiraffers.restaurant.common.exception.AiUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/* 설명. OpenAI Responses API 를 호출하는 부품
 *  ----------------------------------------------------------------------------------
 *  Spring의 WebMvc 의존성에 존재하는 RestClient 로 HTTP 요청을 직접 보낸다.
 *  그래서 OpenAI 에 무엇을 보내고 무엇을 받는지가 코드에 그대로 드러난다.
 *      POST https://api.openai.com/v1/responses
 *      Authorization: Bearer {API 키}
 *      { "model": "...", "instructions": "...", "input": [ {role, content}, ... ] }
 *  ----------------------------------------------------------------------------------
 *  이 클래스가 제공하는 두 가지 호출
 *  1) createJson() : 응답을 한 번에 받는다. JSON Schema 를 주고 그 모양의 JSON 으로만 응답하게 한다. (구조화 출력)
 *  2) streamText() : 응답을 조금씩 받는다. "stream": true 로 보내면 OpenAI 가 SSE 로 글자 조각을 흘려보낸다.
 *  ----------------------------------------------------------------------------------
 *  매 호출마다 모델, 토큰 수, 걸린 시간을 콘솔에 찍는다. 토큰 수가 곧 비용이다.
 *  OpenAI 의 요청, 응답 모양을 아는 곳은 이 클래스뿐이다. 서비스는 createJson(), streamText() 만 호출한다.
 *  주석의 [보조 메서드] 는 이 클래스 안의 private 메서드를 호출하는 곳이다.
 * */
@Component
public class OpenAiClient {

    // OpenAI 서버에 요청을 보내는 HTTP 클라이언트 (주소와 인증 헤더를 미리 넣어 둔다)
    private final RestClient restClient;
    // JSON 문자열 <-> JsonNode 변환 도구 (Spring Boot 가 만들어 둔 빈)
    private final ObjectMapper objectMapper;
    // API 키가 설정되어 있는가. 없으면 AI 기능만 503 으로 막는다.
    private final boolean enabled;

    public OpenAiClient(ObjectMapper objectMapper,
                        // application.yaml 의 openai.api-key. 환경 변수 OPENAI_API_KEY 의 값이 들어온다. (없으면 빈 문자열)
                        @Value("${openai.api-key}") String apiKey,
                        // application.yaml 의 openai.base-url (https://api.openai.com/v1)
                        @Value("${openai.base-url}") String baseUrl) {

        this.objectMapper = objectMapper;
        // 키가 비어 있지 않으면 AI 기능을 켠다.
        this.enabled = apiKey != null && !apiKey.isBlank();

        /* 설명. 응답을 기다리는 시간의 상한을 정한다.
         *  - connectTimeout : OpenAI 서버에 연결하는 데까지 5초
         *  - readTimeout    : 요청을 보내고 응답이 시작되기까지 60초 (스트리밍은 시작된 뒤에는 조각이 계속 온다)
         *  정하지 않으면 OpenAI 쪽이 멈췄을 때 우리 서버의 스레드가 끝없이 기다리게 된다.
         * */
        // 연결 타임아웃은 Java 에 들어 있는 HttpClient 에 정한다
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        // 응답이 시작되기까지의 타임아웃은 요청 부품(requestFactory)에 정한다
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        /* 설명. RestClient.builder() 는 Spring Framework 가 제공하는 정적 메서드라 따로 의존성을 추가하지 않아도 된다.
         *  (Spring Boot 4 에서 미리 설정된 RestClient.Builder 빈을 주입받으려면 spring-boot-starter-restclient 가 따로 필요하다)
         * */
        this.restClient = RestClient.builder()
                // OpenAI 서버의 기본 주소. 요청할 때는 uri("/responses") 처럼 뒷부분만 적는다.
                .baseUrl(baseUrl)
                // 모든 요청에 'Authorization: Bearer {API 키}' 헤더를 붙인다. (Phase 2 의 JWT 와 같은 Bearer 방식)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                // requestFactory 설정 (위에서 정한 타임아웃을 적용한다)
                .requestFactory(requestFactory)
                // 클라이언트 생성
                .build();

        // 키 값은 절대 로그에 찍지 않는다. 있는지 없는지만 확인한다.
        System.out.println(enabled
                ? "[OpenAiClient] OPENAI_API_KEY 확인됨 -> AI 기능을 쓸 수 있다"
                : "[OpenAiClient] OPENAI_API_KEY 가 없다 -> AI 기능은 503 으로 응답한다 (나머지 기능은 정상)");
    }

    /* 설명. 현재 OpenAI API 키가 설정되어 있는지 확인 용도 */
    public boolean isEnabled() {
        return enabled;
    }

    /* 목차. 1. 한 번에 받기 : 구조화 출력(JSON) */
    /**
     * 정해 준 JSON Schema 모양으로만 응답하게 하고, 그 JSON 을 돌려준다.
     *
     * @param model           사용할 모델 (application.yaml 의 openai.model.*)
     * @param reasoningEffort 추론(생각) 정도. 빈 값이면 보내지 않는다 (application.yaml 의 openai.reasoning-effort.*)
     * @param instructions    AI 에게 주는 지시 (역할, 규칙)
     * @param messages        대화 내용
     * @param maxOutputTokens 출력 토큰 상한
     * @param schemaName      스키마 이름 (영문, OpenAI 쪽 로그에 남는 이름)
     * @param schema          응답의 모양을 정한 JSON Schema
     */
    public JsonNode createJson(String model, String reasoningEffort, String instructions, List<ChatMessage> messages,
                               int maxOutputTokens, String schemaName, Map<String, Object> schema) {

        // [보조 메서드] requestBody() : 두 호출에 공통인 필드(model, instructions, input 등)로 요청 본문을 만든다.
        Map<String, Object> body = requestBody(model, reasoningEffort, instructions, messages, maxOutputTokens);

        /* 설명. 구조화 출력(Structured Outputs) 설정
         *  - type   : json_schema  -> 아래 schema 모양의 JSON 으로만 응답한다
         *  - strict : true         -> 스키마에 없는 필드를 만들거나 필수 필드를 빼먹지 않도록 강제한다
         *  덕분에 "응답을 JSON 으로 해줘"라고 부탁만 할 때처럼 모양이 틀어질 걱정이 없다.
         * */
        // 공통 본문에 text.format 을 더한다. (구조화 출력에만 있는 부분)
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", schemaName,
                "strict", true,
                "schema", schema)));

        System.out.println("[OpenAiClient] 요청 (구조화 출력) : model=" + model);
        // 걸린 시간을 재기 위해 시작 시각을 기억해 둔다. (logUsage 에서 쓴다)
        long start = System.currentTimeMillis();

        // 응답 본문을 문자열로 받고, 오류 상태 코드(4xx, 5xx)면 알기 쉬운 메시지의 예외로 바꾼다.
        String responseBody = restClient.post()
                // baseUrl 뒤에 붙는다 -> https://api.openai.com/v1/responses
                .uri("/responses")
                // 요청 본문은 JSON 이다
                .contentType(MediaType.APPLICATION_JSON)
                // Map 을 넘기면 Jackson 이 JSON 으로 바꿔 보낸다
                .body(body)
                // 요청을 보내고, 응답을 다 받을 때까지 기다린다
                .retrieve()
                // 4xx, 5xx 면 [보조 메서드] toException() 으로 우리 예외로 바꿔 던진다
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw toException(response.getStatusCode(), response.getBody());
                })
                // 응답 본문 전체를 문자열로 받는다
                .body(String.class);

        // 응답 문자열을 JsonNode(키와 값의 트리)로 읽는다
        JsonNode response = objectMapper.readTree(responseBody);
        // [보조 메서드] logUsage() : 응답의 usage 에서 토큰 수를 꺼내 걸린 시간과 함께 찍는다. (토큰 수 = 비용)
        logUsage(model, response, start);

        // [보조 메서드] outputText() : 응답의 output 배열에서 AI 가 쓴 글만 꺼낸다.
        // 그 글은 JSON 이 들어 있는 '문자열'이므로, 한 번 더 JsonNode 로 읽어 돌려준다.
        return objectMapper.readTree(outputText(response));
    }

    /* 목차. 2. 조금씩 받기 : 스트리밍 */
    /**
     * "stream": true 로 요청하고, 글자 조각이 올 때마다 onDelta 를 호출한다.
     * 모든 조각을 받으면 돌아온다. onDelta 에서 예외가 나면 읽기를 멈추고 연결을 끊는다.
     *
     * @param onDelta 글자 조각을 받을 함수 (예: 브라우저로 SSE 이벤트를 보내는 함수)
     */
    public void streamText(String model, String reasoningEffort, String instructions, List<ChatMessage> messages,
                           int maxOutputTokens, Consumer<String> onDelta) {

        // [보조 메서드] requestBody() : 두 호출에 공통인 필드로 요청 본문을 만든다.
        Map<String, Object> body = requestBody(model, reasoningEffort, instructions, messages, maxOutputTokens);
        // 응답을 SSE 로 조금씩 보내 달라고 요청한다. (스트리밍에만 있는 부분)
        body.put("stream", true);

        System.out.println("[OpenAiClient] 요청 (스트리밍) : model=" + model);
        // 걸린 시간을 재기 위해 시작 시각을 기억해 둔다. (logUsage 에서 쓴다)
        long start = System.currentTimeMillis();

        /* 설명. retrieve() 는 응답을 다 받은 뒤에 돌려주므로 스트리밍에는 맞지 않는다.
         *  exchange() 는 응답이 시작되자마자 본문(InputStream)을 넘겨주므로, 도착하는 대로 한 줄씩 읽을 수 있다.
         * */
        restClient.post()
                // baseUrl 뒤에 붙는다 -> https://api.openai.com/v1/responses
                .uri("/responses")
                // 요청 본문은 JSON 이다
                .contentType(MediaType.APPLICATION_JSON)
                // 응답으로 text/event-stream(SSE) 을 받겠다고 알린다
                .accept(MediaType.TEXT_EVENT_STREAM)
                // Map 을 넘기면 Jackson 이 JSON 으로 바꿔 보낸다
                .body(body)
                // 응답이 시작되자마자 이 람다가 호출된다. 람다가 끝날 때까지(스트림을 다 읽을 때까지) 이 메서드는 돌아가지 않는다.
                .exchange((request, response) -> {

                    // 오류 상태 코드면 본문을 읽지 않고 [보조 메서드] toException() 으로 우리 예외로 바꿔 던진다
                    if (response.getStatusCode().isError()) {
                        throw toException(response.getStatusCode(), response.getBody());
                    }

                    /* 설명. OpenAI 가 보내는 SSE 는 이런 줄들이 이어진다. (빈 줄 하나가 이벤트 하나의 끝)
                     *      event: response.output_text.delta
                     *      data: {"type":"response.output_text.delta","delta":"오늘은",...}
                     *  data: 뒤의 JSON 에서 type 을 보고, 글자 조각(delta)만 꺼내 넘긴다.
                     * */
                    // 응답 본문(InputStream)을 UTF-8 글자로 한 줄씩 읽는다. try 블록이 끝나면 자동으로 닫힌다.
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        // 받은 글자 조각 수 (로그용)
                        int deltaCount = 0;
                        // 다음 줄이 도착할 때까지 기다렸다가 읽는다. OpenAI 가 응답을 끝내면 null 이 되어 반복이 끝난다.
                        while ((line = reader.readLine()) != null) {

                            // event: 줄과 빈 줄은 건너뛴다. 필요한 정보는 data: 줄의 JSON 에 모두 들어 있다.
                            if (!line.startsWith("data: ")) {
                                continue;
                            }

                            // 'data: ' 뒤의 JSON 을 읽고
                            JsonNode event = objectMapper.readTree(line.substring("data: ".length()));
                            // 이벤트 종류(type)를 꺼낸다
                            String type = event.path("type").asString();

                            switch (type) {
                                // 글자 조각이 도착했다 -> 바로 넘긴다
                                case "response.output_text.delta" -> {
                                    deltaCount++;
                                    // 호출한 쪽이 넘겨준 함수(onDelta)에 조각을 넘긴다.
                                    // 추천 채팅이라면 RecommendationService 의 람다가 이 조각을 브라우저로 보낸다.
                                    onDelta.accept(event.path("delta").asString());
                                }
                                // 다 만들었다 -> 마지막 이벤트에 토큰 사용량이 들어 있다
                                case "response.completed", "response.incomplete" -> {
                                    System.out.println("[OpenAiClient] 스트리밍 조각 " + deltaCount + "개 받음");
                                    // [보조 메서드] logUsage() : 마지막 이벤트의 response.usage 에서 토큰 수를 꺼내 찍는다
                                    logUsage(model, event.path("response"), start);
                                }
                                // 만드는 도중에 실패했다
                                case "response.failed", "error" -> throw new AiRequestException(
                                        "AI 응답 생성에 실패했습니다. " + event.path("response").path("error").path("message").asString(""));
                                default -> {
                                    // 그 밖의 이벤트(response.created 등)는 쓰지 않는다.
                                }
                            }
                        }
                    }
                    // 돌려줄 값은 없다. 글자는 onDelta 로 이미 모두 넘겼다.
                    return null;
                });
    }

    /* 목차. 3. [Phase 3-2 선택] 이미지 만들기 */
    /**
     * Images API 로 이미지 한 장을 만들고, 완성된 이미지를 Base64 문자열로 돌려준다.
     * "stream": true 로 요청하면 중간 미리보기(partial_image)가 올 수 있다. 올 때마다 onPartial 을 호출한다.
     * (quality 가 low 이면 미리보기 없이 완성본만 오기도 한다)
     *
     *     POST https://api.openai.com/v1/images/generations
     *     { "model": "...", "prompt": "...", "size": "1024x1024", "quality": "low", "stream": true, "partial_images": 2 }
     */
    public String streamImage(String model, String prompt, String size, String quality, Consumer<String> onPartial) {

        // Images API 는 Responses API 와 요청 모양이 달라서 requestBody() 를 쓰지 않고 따로 만든다.
        Map<String, Object> body = new LinkedHashMap<>();
        // 사용할 이미지 모델
        body.put("model", model);
        // 만들 그림의 설명
        body.put("prompt", prompt);
        // 이미지 크기. 클수록 비싸다
        body.put("size", size);
        // low, medium, high. 높을수록 오래 걸리고 비싸다
        body.put("quality", quality);
        // jpeg 가 png 보다 훨씬 작다. (WebSocket 으로 화면에 보내야 하므로 작을수록 좋다)
        body.put("output_format", "jpeg");
        // 만드는 도중의 미리보기를 SSE 로 받는다
        body.put("stream", true);
        // 미리보기를 최대 몇 장 받을지
        body.put("partial_images", 2);

        System.out.println("[OpenAiClient] 요청 (이미지) : model=" + model + ", size=" + size + ", quality=" + quality);
        // 걸린 시간을 재기 위해 시작 시각을 기억해 둔다.
        long start = System.currentTimeMillis();

        return restClient.post()
                // baseUrl 뒤에 붙는다 -> https://api.openai.com/v1/images/generations
                .uri("/images/generations")
                // 요청 본문은 JSON 이다
                .contentType(MediaType.APPLICATION_JSON)
                // 응답으로 text/event-stream(SSE) 을 받겠다고 알린다
                .accept(MediaType.TEXT_EVENT_STREAM)
                // Map 을 넘기면 Jackson 이 JSON 으로 바꿔 보낸다
                .body(body)
                // 응답이 시작되자마자 이 람다가 호출된다. 완성 이미지를 받으면 그 값을 이 메서드의 반환값으로 돌려준다.
                .exchange((request, response) -> {

                    // 오류 상태 코드면 본문을 읽지 않고 [보조 메서드] toException() 으로 우리 예외로 바꿔 던진다
                    if (response.getStatusCode().isError()) {
                        throw toException(response.getStatusCode(), response.getBody());
                    }

                    // 응답 본문(InputStream)을 UTF-8 글자로 한 줄씩 읽는다. try 블록이 끝나면 자동으로 닫힌다.
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        // 다음 줄이 도착할 때까지 기다렸다가 읽는다. OpenAI 가 응답을 끝내면 null 이 되어 반복이 끝난다.
                        while ((line = reader.readLine()) != null) {

                            // event: 줄과 빈 줄은 건너뛴다. 필요한 정보는 data: 줄의 JSON 에 모두 들어 있다.
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            // 'data:' 뒤의 JSON 을 읽고
                            JsonNode event = objectMapper.readTree(line.substring("data:".length()).trim());
                            // 이벤트 종류(type)를 꺼낸다
                            String type = event.path("type").asString();

                            switch (type) {
                                // 중간 미리보기 (흐릿한 이미지) -> 호출한 쪽이 넘겨준 함수(onPartial)에 넘긴다
                                case "image_generation.partial_image" -> onPartial.accept(event.path("b64_json").asString());
                                // 완성 : 이미지와 토큰 사용량이 함께 온다
                                case "image_generation.completed" -> {
                                    // 이미지는 usage 의 모양이 달라서 logUsage() 를 쓰지 않고 여기서 찍는다
                                    JsonNode usage = event.path("usage");
                                    System.out.println("[OpenAiClient] 완료 (이미지) : model=" + model
                                            + ", 입력 토큰=" + usage.path("input_tokens").asInt()
                                            + ", 출력(이미지) 토큰=" + usage.path("output_tokens").asInt()
                                            + ", " + (System.currentTimeMillis() - start) + "ms");
                                    // 완성 이미지(Base64 문자열)를 돌려준다
                                    return event.path("b64_json").asString();
                                }
                                // 만드는 도중에 실패했다
                                case "error" -> throw new AiRequestException(
                                        "이미지 생성에 실패했습니다. " + event.path("error").path("message").asString(""));
                                default -> {
                                    // 그 밖의 이벤트는 쓰지 않는다.
                                }
                            }
                        }
                    }
                    // 완성 이벤트 없이 스트림이 끝났다
                    throw new AiRequestException("이미지 생성 응답이 끝까지 오지 않았습니다.");
                });
    }

    /* 설명. 두 호출이 함께 쓰는 요청 본문
     *  - instructions      : AI 의 역할과 규칙. 대화(input)와 따로 보낸다.
     *  - input             : 대화 내용 [{role, content}, ...]
     *  - max_output_tokens : 출력 토큰 상한. 넘으면 응답이 중간에 끊긴다(status = incomplete).
     *  - store             : false 면 OpenAI 가 이 대화를 저장하지 않는다. (기본값은 true, 30일 보관)
     *  - reasoning.effort  : 추론 모델이 응답하기 전에 얼마나 '생각'할지. 생각한 토큰(추론 토큰)도 출력 요금으로 청구되고 시간도 걸린다.
     *                        쓸 수 있는 값이 모델마다 다르다. (예: gpt-6-luna 는 none/low/medium, gpt-5-nano 는 minimal/low/medium)
     *                        추론 기능이 없는 모델(gpt-4.1-nano 등)에 보내면 오류가 나므로, 빈 값이면 아예 보내지 않는다.
     * */
    private Map<String, Object> requestBody(String model, String reasoningEffort, String instructions,
                                            List<ChatMessage> messages, int maxOutputTokens) {

        // ChatMessage 목록을 OpenAI 의 input 모양 [{ "role": ..., "content": ... }, ...] 으로 바꾼다
        List<Map<String, String>> input = messages.stream()
                .map(message -> Map.of("role", message.getRole(), "content", message.getContent()))
                .toList();

        // 순서를 지키는 Map. 로그나 디버깅 화면에서 보기 편하다.
        Map<String, Object> body = new LinkedHashMap<>();
        // 사용할 모델
        body.put("model", model);
        // AI 의 역할과 규칙 (시스템 프롬프트)
        body.put("instructions", instructions);
        // 대화 내용
        body.put("input", input);
        // 출력 토큰 상한
        body.put("max_output_tokens", maxOutputTokens);
        // OpenAI 서버에 대화를 저장하지 않는다. (대화는 우리가 직접 골라 보낸다)
        body.put("store", false);
        // 추론 정도는 값이 있을 때만 넣는다
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            body.put("reasoning", Map.of("effort", reasoningEffort));
        }
        return body;
    }

    /* 설명. 응답에서 AI 가 쓴 글을 꺼낸다.
     *  응답의 output 은 여러 항목의 목록이다. (추론 과정 reasoning, 실제 응답 message 등)
     *  그중 type 이 message 인 항목의 content 에서 type 이 output_text 인 글만 이어 붙인다.
     * */
    private String outputText(JsonNode response) {

        // 출력 토큰 상한에 걸려 끊긴 응답이면 JSON 이 중간에 잘려 있으므로, 쓰지 않고 예외로 알린다
        if ("incomplete".equals(response.path("status").asString())) {
            throw new AiRequestException("AI 응답이 출력 토큰 상한에서 끊겼습니다. (max-output-tokens 를 늘려 보세요)");
        }

        // AI 가 쓴 글을 이어 붙일 곳
        StringBuilder text = new StringBuilder();
        // output 배열의 항목(reasoning, message ...)을 하나씩 본다
        for (JsonNode item : response.path("output")) {
            // 실제 응답이 들어 있는 항목은 type 이 message 인 항목이다
            if ("message".equals(item.path("type").asString())) {
                // message 의 content 배열을 하나씩 본다
                for (JsonNode content : item.path("content")) {
                    // 글은 type 이 output_text 인 항목의 text 에 들어 있다
                    if ("output_text".equals(content.path("type").asString())) {
                        text.append(content.path("text").asString());
                    }
                }
            }
        }
        return text.toString();
    }

    /* 설명. 토큰 사용량과 걸린 시간을 찍는다.
     *  비용 = 입력 토큰 x 입력 단가 + 출력 토큰 x 출력 단가. (추론 토큰은 출력 토큰에 포함되어 청구된다)
     * */
    private void logUsage(String model, JsonNode response, long start) {

        // 응답의 usage 객체에 토큰 수가 들어 있다
        JsonNode usage = response.path("usage");
        System.out.println("[OpenAiClient] 완료 : model=" + model
                // 우리가 보낸 입력 토큰
                + ", 입력 토큰=" + usage.path("input_tokens").asInt()
                // AI 가 만든 출력 토큰 (추론 토큰 포함)
                + ", 출력 토큰=" + usage.path("output_tokens").asInt()
                // 출력 토큰 중 추론에 쓴 토큰
                + " (그중 추론 " + usage.path("output_tokens_details").path("reasoning_tokens").asInt() + ")"
                // 요청부터 지금까지 걸린 시간
                + ", " + (System.currentTimeMillis() - start) + "ms");
    }

    /* 설명. OpenAI 의 오류 응답을 우리가 이해하기 쉬운 메시지로 바꾼다.
     *  오류 본문 모양 : { "error": { "message": "...", "type": "...", "code": "..." } }
     * */
    private RuntimeException toException(HttpStatusCode status, InputStream errorBody) throws IOException {

        // 오류 응답 본문을 문자열로 읽는다
        String body = new String(errorBody.readAllBytes(), StandardCharsets.UTF_8);
        String message;
        try {
            // error.message 만 꺼낸다. 없으면 본문 전체를 쓴다
            message = objectMapper.readTree(body).path("error").path("message").asString(body);
        } catch (RuntimeException e) {
            // 본문이 JSON 이 아니면 본문을 그대로 쓴다
            message = body;
        }

        // 원인을 찾을 수 있게 OpenAI 가 보낸 상태 코드와 메시지를 그대로 찍는다
        System.out.println("[OpenAiClient] 실패 : HTTP " + status.value() + " - " + message);

        /* 설명. 상태 코드에 따라 예외 종류를 고른다.
         *  - AiUnavailableException : 우리 서버의 설정 문제 (GlobalExceptionHandler 가 503 으로 응답한다)
         *  - AiRequestException     : 우리가 호출한 OpenAI 쪽의 실패 (GlobalExceptionHandler 가 502 로 응답한다)
         *  추천 채팅처럼 SSE 를 이미 연 뒤라면 상태 코드는 바꿀 수 없으므로, 서비스가 이 예외의 메시지를 error 이벤트로 보낸다.
         * */
        return switch (status.value()) {
            // 키가 틀렸다 : 운영자가 고쳐야 하는 설정 문제.
            // OpenAI 의 401 은 우리 서비스의 로그인과 관계가 없으므로 401 로 넘기지 않는다.
            case 401 -> new AiUnavailableException("OpenAI API 키가 올바르지 않습니다. OPENAI_API_KEY 를 확인하세요.");
            // 이 모델을 쓸 권한이 없다
            case 403 -> new AiRequestException("이 모델을 쓸 권한이 없습니다. (조직 인증이 필요한 모델일 수 있습니다) " + message);
            // 요청 한도를 넘었거나 충전 잔액이 부족하다
            case 429 -> new AiRequestException("OpenAI 요청 한도를 넘었거나 충전 잔액이 부족합니다. " + message);
            // 그 밖의 실패 (5xx 등)
            default -> new AiRequestException("OpenAI 호출 실패 (HTTP " + status.value() + ") " + message);
        };
    }
}
