package com.ohgiraffers.restaurant.menu.controller;

import com.ohgiraffers.restaurant.common.PageResponse;
import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.menu.dto.MenuDetailResponse;
import com.ohgiraffers.restaurant.menu.dto.MenuResponse;
import com.ohgiraffers.restaurant.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "메뉴")
@SecurityRequirements   // [Phase 2] 토큰 없이 누구나 요청할 수 있는 API 라고 Swagger 에 표시한다
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /* 목차. 1. 메뉴 목록 조회 (손님용) */
    /* 설명. 요청 예시
     *  - GET /api/menus                          : 주문 가능한 전체 메뉴 1페이지
     *  - GET /api/menus?page=2&size=8            : 2페이지 (한 페이지에 8개)
     *  - GET /api/menus?categoryCode=2           : 음료(하위 카테고리 포함)
     *  - GET /api/menus?keyword=스무디            : 메뉴명에 '스무디'가 들어간 메뉴
     *  categoryCode와 keyword는 선택 값이므로 required = false로 받는다. (보내지 않으면 null)
     * */
    @Operation(summary = "메뉴 목록 (손님용)", description = """
            주문 가능한 메뉴만 등록 순서(menuCode 오름차순)로 조회한다. page는 1부터 시작하고 기본 size는 8이다.
            - 200 : result.menuPage
              - content : 배열. menuCode, menuName, menuPrice, categoryCode, categoryName,
                          orderableStatus('Y'/'N'), menuDescription, menuImageUrl
              - totalElements, totalPages, size, number(1부터), first, last
            menuImageUrl은 '/uploads/menus/파일명' 형태의 경로이며, 서버 주소를 앞에 붙여 쓴다. 이미지가 없으면 null이다.
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findMenus(
            @Parameter(description = "카테고리 코드 (상위 카테고리면 하위 카테고리 포함)")
            @RequestParam(required = false) Integer categoryCode,
            @Parameter(description = "메뉴명 검색어")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 8) Pageable pageable
    ) {

        Page<MenuResponse> menuPage = menuService.findOrderableMenus(categoryCode, keyword, pageable);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "메뉴 목록 조회 성공",
                        Map.of("menuPage", PageResponse.from(menuPage))));
    }

    /* 목차. 2. 메뉴 상세 조회 */
    @Operation(summary = "메뉴 상세", description = """
            - 200 : result.menu (목록 항목의 값 + menuIngredients, allergens[allergenCode, allergenName])
            - 404 : 존재하지 않거나 삭제된 메뉴
            주문 불가('N') 메뉴도 상세 조회는 된다.
            """)
    @GetMapping("/{menuCode}")
    public ResponseEntity<ResponseMessage> findMenuDetail(@PathVariable int menuCode) {

        MenuDetailResponse menu = menuService.findMenuDetail(menuCode);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "메뉴 상세 조회 성공", Map.of("menu", menu)));
    }
}
