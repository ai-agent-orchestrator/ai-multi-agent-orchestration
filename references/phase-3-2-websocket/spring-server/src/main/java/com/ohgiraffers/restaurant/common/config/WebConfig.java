package com.ohgiraffers.restaurant.common.config;

import com.ohgiraffers.restaurant.common.file.FileStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/* 설명. 외부 폴더에 저장된 업로드 파일을 URL로 꺼내볼 수 있게 연결하는 설정
 *  04_spring_boot chap06(file upload)의 WebConfig와 같은 방식이다.
 *  ----------------------------------------------------------------------------------
 *  예) 파일 위치 : C:/myWs/uploadedFiles/menus/06a0...5e.PNG
 *      접근 URL  : http://localhost:8080/uploads/menus/06a0...5e.PNG
 *  ----------------------------------------------------------------------------------
 *  React는 이 URL을 <img src="...">에 그대로 넣어 이미지를 보여준다.
 *  참고. <img> 태그로 이미지를 불러오는 것은 CORS 검사 대상이 아니다.
 *       (JavaScript가 응답 내용을 직접 읽는 fetch/axios 요청만 CORS 검사를 받는다)
 * */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorage fileStorage;

    public WebConfig(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String uploadPath = fileStorage.getBasePath();

        System.out.println("파일 업로드 경로 : " + uploadPath);

        /* 설명. file:// 프로토콜은 OS의 파일 시스템에 접근하는 프로토콜이다. */
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + uploadPath + "/");
    }
}
