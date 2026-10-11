package com.ohgiraffers.restaurant.menu.controller;

import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.menu.dto.CategoryResponse;
import com.ohgiraffers.restaurant.menu.service.CategoryService;
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

@Tag(name = "메뉴", description = "카테고리 · 알레르기 · 메뉴 조회 (손님용)")
@SecurityRequirements   // [Phase 2] 토큰 없이 누구나 요청할 수 있는 API 라고 Swagger 에 표시한다
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /* 목차. 1. 전체 카테고리 조회 */
    @Operation(summary = "카테고리 목록", description = """
            - 200 : result.categories (배열. categoryCode, categoryName, refCategoryCode)
            refCategoryCode가 null이면 최상위 카테고리(1 식사, 2 음료, 3 디저트)다.
            메뉴는 하위 카테고리에 속하지만 일부 메뉴는 최상위 카테고리에 바로 속한다.
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findAllCategories() {

        List<CategoryResponse> categories = categoryService.findAllCategories();

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "카테고리 목록 조회 성공", Map.of("categories", categories)));
    }
}
