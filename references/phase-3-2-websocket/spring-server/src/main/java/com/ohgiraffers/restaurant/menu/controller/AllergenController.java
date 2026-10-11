package com.ohgiraffers.restaurant.menu.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.menu.dto.AllergenResponse;
import com.ohgiraffers.restaurant.menu.service.AllergenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "메뉴")
@SecurityRequirements   // [Phase 2] 토큰 없이 누구나 요청할 수 있는 API 라고 Swagger 에 표시한다
@RestController
@RequestMapping("/api/allergens")
public class AllergenController {

    private final AllergenService allergenService;

    public AllergenController(AllergenService allergenService) {
        this.allergenService = allergenService;
    }

    /* 목차. 1. 전체 알레르기 조회 */
    @Operation(summary = "알레르기 목록", description = """
            관리자 메뉴 등록·수정 화면의 체크박스 목록으로 사용한다.
            - 200 : result.allergens (배열. allergenCode, allergenName)
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findAllAllergens() {

        List<AllergenResponse> allergens = allergenService.findAllAllergens();

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "알레르기 목록 조회 성공", Map.of("allergens", allergens)));
    }
}
