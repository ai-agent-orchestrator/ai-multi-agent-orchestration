package com.ohgiraffers.restaurant.menu.controller;

import com.ohgiraffers.restaurant.common.PageResponse;
import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.menu.dto.MenuDetailResponse;
import com.ohgiraffers.restaurant.menu.dto.MenuRequest;
import com.ohgiraffers.restaurant.menu.dto.MenuResponse;
import com.ohgiraffers.restaurant.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/* 설명. 관리자 전용 API는 모두 /api/admin 아래에 모아둔다.
 *  ----------------------------------------------------------------------------------
 *  [Phase 2] SecurityConfig 의 메뉴(menu) 블록에 있는 규칙 한 줄로 이 주소 전체를 막는다.
 *      .requestMatchers("/api/admin/menus", "/api/admin/menus/**").hasRole("ADMIN")
 *  - 토큰 없이 요청        -> 401 (누구인지 모름)
 *  - ROLE_USER 토큰으로 요청 -> 403 (누구인지는 알지만 권한 없음)
 *  주소를 한곳에 모아두었기 때문에 이 컨트롤러의 코드는 한 줄도 바꾸지 않아도 된다.
 * */
@Tag(name = "메뉴 관리 (관리자)", description = "메뉴 등록 · 수정 · 삭제")
@RestController
@RequestMapping("/api/admin/menus")
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /* 목차. 1. 관리자용 메뉴 목록 조회 (주문 불가 메뉴 포함) */
    @Operation(summary = "메뉴 목록 (관리자용)", description = """
            주문 불가('N') 메뉴까지 포함한다. 삭제된 메뉴는 제외한다. 최근 등록순이며 page는 1부터, 기본 size는 10이다.
            - 200 : result.menuPage (손님용 메뉴 목록과 같은 모양)
            - 401 : 토큰 없음·만료 / 403 : 관리자가 아님
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findMenusForAdmin(@PageableDefault(size = 10) Pageable pageable) {

        Page<MenuResponse> menuPage = menuService.findMenusForAdmin(pageable);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "관리자 메뉴 목록 조회 성공",
                        Map.of("menuPage", PageResponse.from(menuPage))));
    }

    /* 목차. 2. 메뉴 등록 */
    /* 설명. 이미지 파일과 메뉴 정보를 multipart/form-data 한 번의 요청으로 받는다.
     *  - 메뉴 정보(텍스트 항목) : @ModelAttribute로 MenuRequest에 채운다.
     *  - 이미지 파일           : MultipartFile 파라미터로 따로 받는다. (form-data 항목 이름: menuImage)
     *  JSON 본문이 아니므로 @RequestBody를 쓰지 않는다는 점에 주의한다.
     * */
    @Operation(summary = "메뉴 등록", description = """
            multipart/form-data로 요청한다. JSON이 아니다.
            항목: menuName, menuPrice, categoryCode, orderableStatus('Y'/'N'), menuDescription, menuIngredients,
                  allergenCodes(같은 이름으로 여러 번), menuImage(이미지 파일, 선택)
            - 201 : result.menu (메뉴 상세와 같은 모양)
            - 400 : 입력값 검증 실패, 없는 카테고리·알레르기 코드, 이미지가 아닌 파일
            - 401 : 토큰 없음·만료 / 403 : 관리자가 아님
            """)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage> registMenu(
            @Valid @ModelAttribute MenuRequest menuRequest,
            @RequestParam(value = "menuImage", required = false) MultipartFile menuImage
    ) {

        MenuDetailResponse menu = menuService.registMenu(menuRequest, menuImage);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseMessage(HttpStatus.CREATED.value(), "메뉴 등록 성공", Map.of("menu", menu)));
    }

    /* 목차. 3. 메뉴 수정 */
    @Operation(summary = "메뉴 수정", description = """
            등록과 같은 항목을 multipart/form-data로 보낸다. 이미지를 보내지 않으면 기존 이미지를 유지한다.
            allergenCodes는 '바뀐 것'이 아니라 '최종 목록 전체'를 보낸다.
            - 200 : result.menu (메뉴 상세와 같은 모양)
            - 400 : 입력값 검증 실패 / 404 : 존재하지 않거나 삭제된 메뉴
            - 401 : 토큰 없음·만료 / 403 : 관리자가 아님
            """)
    @PutMapping(value = "/{menuCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage> modifyMenu(
            @PathVariable int menuCode,
            @Valid @ModelAttribute MenuRequest menuRequest,
            @RequestParam(value = "menuImage", required = false) MultipartFile menuImage
    ) {

        MenuDetailResponse menu = menuService.modifyMenu(menuCode, menuRequest, menuImage);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "메뉴 수정 성공", Map.of("menu", menu)));
    }

    /* 목차. 4. 메뉴 삭제 (soft delete) */
    /* 설명. 클라이언트 입장에서는 평범한 DELETE 요청이지만, 서버는 행을 지우지 않고 삭제 표시만 한다.
     *  API는 '무엇을 하고 싶은지(삭제)'를 표현하고, 그것을 '어떻게 처리할지'는 서버가 정한다.
     * */
    @Operation(summary = "메뉴 삭제", description = """
            실제로 지우지 않고 삭제 표시(soft delete)만 한다. 이후 목록과 상세 조회에서 사라진다.
            - 200 : result.deletedMenuCode (204가 아니라 200으로 응답한다)
            - 404 : 존재하지 않거나 이미 삭제된 메뉴
            - 401 : 토큰 없음·만료 / 403 : 관리자가 아님
            """)
    @DeleteMapping("/{menuCode}")
    public ResponseEntity<ResponseMessage> removeMenu(@PathVariable int menuCode) {

        menuService.removeMenu(menuCode);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "메뉴 삭제 성공", Map.of("deletedMenuCode", menuCode)));
    }
}
