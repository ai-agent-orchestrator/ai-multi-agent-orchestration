package com.ohgiraffers.restaurant.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/* 설명. CORS (Cross-Origin Resource Sharing) 설정 (04_spring_data_jpa chap06과 같은 설정)
 *  ----------------------------------------------------------------------------------
 *  - 프론트엔드(React)  : http://localhost:5173
 *  - 백엔드(Spring Boot): http://localhost:8080
 *  두 서버의 포트가 다르므로 브라우저는 이를 '다른 오리진(Cross-Origin)'으로 판단하고,
 *  서버가 허락하지 않은 응답은 JavaScript가 읽지 못하도록 막는다.
 *  아래 설정은 "http://localhost:5173 오리진의 요청은 허용한다"고 브라우저에게 알려주는 역할을 한다.
 *  ----------------------------------------------------------------------------------
 *  주의. Vite가 5173 포트를 이미 쓰고 있으면 5174 포트로 실행되는데, 이때는 CORS 오류가 난다.
 *       React 쪽 vite.config.js에 strictPort: true를 두어 포트가 바뀌지 않게 막아두었다.
 *  참고. chap06 설정에 있던 allowCredentials(true)는 뺐다. 쿠키를 주고받을 때만 필요한 설정인데,
 *       이 프로젝트는 쿠키로 로그인 상태를 유지하지 않는다.
 * */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // 모든 경로(/**)에 대해 CORS 설정을 적용한다.
        registry.addMapping("/**")
                // 요청을 허용할 오리진(도메인+포트)을 지정한다. (Vite 기본 포트인 5173 허가)
                .allowedOrigins("http://localhost:5173")
                // 허용할 HTTP 메서드를 지정한다. (RESTful API의 주요 메서드들)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 모든 헤더 정보를 허용한다.
                .allowedHeaders("*");
    }
}
