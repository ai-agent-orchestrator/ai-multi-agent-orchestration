package com.ohgiraffers.restaurant.cart.controller;

import com.ohgiraffers.restaurant.cart.dto.CartAmountRequest;
import com.ohgiraffers.restaurant.cart.dto.CartItemRequest;
import com.ohgiraffers.restaurant.cart.dto.CartItemResponse;
import com.ohgiraffers.restaurant.cart.service.CartService;
import com.ohgiraffers.restaurant.common.ResponseMessage;
import com.ohgiraffers.restaurant.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/* 설명. [Phase 2] /api/members/{memberCode}/cart 가 /api/members/me/cart 로 바뀌었다.
 *  장바구니의 주인은 주소의 숫자가 아니라 토큰의 주인이다. 서비스 코드는 Phase 1 과 같다.
 * */
@Tag(name = "장바구니", description = "장바구니 조회 · 담기 · 수량 변경 · 빼기")
@RestController
@RequestMapping("/api/members/me/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /* 목차. 1. 장바구니 조회 */
    @Operation(summary = "장바구니 조회", description = """
            토큰 주인의 장바구니를 담은 순서대로 조회한다. 삭제된 메뉴는 빠진다.
            - 200 : result.cartItems (배열. cartCode, menuCode, menuName, menuPrice, orderableStatus, menuImageUrl, cartAmount)
            - 401 : 토큰 없음·만료
            """)
    @GetMapping
    public ResponseEntity<ResponseMessage> findCart(@AuthenticationPrincipal CustomUserDetails user) {

        List<CartItemResponse> cartItems = cartService.findCart(user.getMemberCode());

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "장바구니 조회 성공", Map.of("cartItems", cartItems)));
    }

    /* 목차. 2. 장바구니 담기 */
    @Operation(summary = "장바구니 담기", description = """
            이미 담긴 메뉴면 수량을 더한다. 한 메뉴는 99개까지 담을 수 있다.
            본문: { menuCode, cartAmount }
            - 200 : result.cartItems (담은 뒤의 장바구니 전체)
            - 400 : 입력값 검증 실패, 주문 불가 메뉴, 99개 초과 / 401 : 토큰 없음·만료 / 404 : 존재하지 않는 메뉴
            """)
    @PostMapping("/items")
    public ResponseEntity<ResponseMessage> addCartItem(@AuthenticationPrincipal CustomUserDetails user,
                                                       @Valid @RequestBody CartItemRequest cartItemRequest) {

        List<CartItemResponse> cartItems = cartService.addCartItem(user.getMemberCode(), cartItemRequest);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "장바구니 담기 성공", Map.of("cartItems", cartItems)));
    }

    /* 목차. 3. 수량 변경 */
    @Operation(summary = "장바구니 수량 변경", description = """
            수량을 본문의 값으로 바꾼다. (더하는 것이 아니다)
            본문: { cartAmount }
            - 200 : result.cartItems (바꾼 뒤의 장바구니 전체)
            - 400 : 입력값 검증 실패 / 401 : 토큰 없음·만료 / 404 : 장바구니에 없는 메뉴
            """)
    @PutMapping("/items/{menuCode}")
    public ResponseEntity<ResponseMessage> changeCartItem(@AuthenticationPrincipal CustomUserDetails user,
                                                          @PathVariable int menuCode,
                                                          @Valid @RequestBody CartAmountRequest cartAmountRequest) {

        List<CartItemResponse> cartItems =
                cartService.changeCartAmount(user.getMemberCode(), menuCode, cartAmountRequest.getCartAmount());

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "장바구니 수량 변경 성공", Map.of("cartItems", cartItems)));
    }

    /* 목차. 4. 장바구니에서 빼기 */
    @Operation(summary = "장바구니에서 빼기", description = """
            - 200 : result.cartItems (뺀 뒤의 장바구니 전체)
            - 401 : 토큰 없음·만료 / 404 : 장바구니에 없는 메뉴
            """)
    @DeleteMapping("/items/{menuCode}")
    public ResponseEntity<ResponseMessage> removeCartItem(@AuthenticationPrincipal CustomUserDetails user,
                                                          @PathVariable int menuCode) {

        List<CartItemResponse> cartItems = cartService.removeCartItem(user.getMemberCode(), menuCode);

        return ResponseEntity
                .ok(new ResponseMessage(HttpStatus.OK.value(), "장바구니에서 빼기 성공", Map.of("cartItems", cartItems)));
    }
}
