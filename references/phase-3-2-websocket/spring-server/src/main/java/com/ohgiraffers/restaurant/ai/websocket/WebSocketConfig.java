package com.ohgiraffers.restaurant.ai.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/* 설명. [Phase 3-2] WebSocket 주소 등록
 *  ----------------------------------------------------------------------------------
 *  WebSocket 은 처음에 평범한 HTTP GET 요청으로 시작한다. ('Upgrade: websocket' 헤더를 붙여 보낸다)
 *  서버가 101 Switching Protocols 로 답하면 그때부터 같은 연결이 WebSocket 으로 바뀌어 계속 열려 있다.
 *  이 클래스는 '어떤 주소로 들어온 연결을 어떤 핸들러가 맡을지'를 정한다.
 *  ----------------------------------------------------------------------------------
 *  메시지 브로커(STOMP)는 쓰지 않는다. 기능이 하나뿐이라 핸들러 하나가 메시지를 직접 받는 순수 WebSocket 으로 충분하다.
 * */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MenuDraftWebSocketHandler menuDraftWebSocketHandler;

    public WebSocketConfig(MenuDraftWebSocketHandler menuDraftWebSocketHandler) {
        this.menuDraftWebSocketHandler = menuDraftWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(menuDraftWebSocketHandler, "/ws/admin/menu-draft")
                // CorsConfig(HTTP 요청용)와 따로 정한다. 다른 출처의 화면이 연결하지 못하게 React 주소만 허용한다.
                .setAllowedOrigins("http://localhost:5173");
    }
}
