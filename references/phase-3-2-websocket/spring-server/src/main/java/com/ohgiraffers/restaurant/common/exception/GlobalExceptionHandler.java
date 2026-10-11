package com.ohgiraffers.restaurant.common.exception;

import com.ohgiraffers.restaurant.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/* 설명. 모든 컨트롤러에서 발생한 예외를 한곳에서 가로채 알맞은 상태 코드로 바꿔주는 전역 예외 처리기
 *  (04_spring_data_jpa chap06의 ExceptionController와 같은 역할이다)
 *  ----------------------------------------------------------------------------------
 *  상태 코드 정리
 *  - 400 Bad Request  : 요청 값이 잘못됨 (입력값 검증 실패, 존재하지 않는 카테고리 코드 등)
 *  - 401 Unauthorized : 로그인 실패
 *  - 404 Not Found    : 요청한 자원이 없음
 *  - 409 Conflict     : 이미 존재하는 값과 충돌함 (중복 아이디)
 *  - 500 Internal Server Error : 위에서 처리하지 못한 나머지
 * */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00001", "조회 실패", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00002", "잘못된 요청 값", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /* 설명. @Valid 입력값 검증에 실패한 경우 -> 400 Bad Request
     *  - @RequestBody(JSON)로 받은 값의 검증 실패 : MethodArgumentNotValidException
     *  - @ModelAttribute(form-data)로 받은 값의 검증 실패 : BindException
     *  MethodArgumentNotValidException이 BindException을 상속하므로 BindException 하나로 둘 다 잡힌다.
     *  여러 필드가 동시에 틀렸더라도 화면에 보여주기 쉽도록 첫 번째 메시지만 담아 보낸다.
     * */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {

        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "입력값을 확인해주세요.";

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00003", "입력값 검증 실패", detail);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ErrorResponse> handleLoginFailedException(LoginFailedException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00004", "로그인 실패", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00005", "중복된 값", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /* 설명. [Phase 3] AI 기능을 쓸 수 없음 (OPENAI_API_KEY 미설정) -> 503 */
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailableException(AiUnavailableException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00010", "AI 기능 사용 불가", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /* 설명. [Phase 3] OpenAI 호출 실패 -> 502 */
    @ExceptionHandler(AiRequestException.class)
    public ResponseEntity<ErrorResponse> handleAiRequestException(AiRequestException e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00011", "AI 응답 실패", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }

    /* 설명. 요청 형식 자체가 잘못된 경우 -> 400 Bad Request
     *  - HttpMessageNotReadableException   : JSON 문법이 틀렸거나 본문을 읽을 수 없음
     *  - MethodArgumentTypeMismatchException : 경로·쿼리의 값 타입이 틀림 (예: /api/menus/abc)
     * */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestFormat(Exception e) {

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00006", "잘못된 요청 형식", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /* 설명. 위에서 처리하지 못한 나머지 예외
     *  ----------------------------------------------------------------------------------
     *  Spring MVC가 던지는 표준 예외 중 상당수는 이미 알맞은 상태 코드를 가지고 있다.
     *  (없는 주소 404, 지원하지 않는 HTTP 메서드 405, 업로드 용량 초과 413 등)
     *  이런 예외는 org.springframework.web.ErrorResponse 인터페이스를 구현하고 있으므로,
     *  모두 500으로 뭉개지 않고 원래의 상태 코드를 그대로 살려서 응답한다.
     *  그 밖의 예외만 500 Internal Server Error로 응답한다.
     *  ----------------------------------------------------------------------------------
     *  주의. 실제 운영 환경에서는 e.getMessage()를 그대로 내려주면 안 된다.
     *       내부 구조나 SQL 정보가 그대로 노출될 수 있기 때문이다.
     *       여기서는 학습 목적으로 원인을 바로 확인할 수 있도록 그대로 담았다.
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        if (e instanceof org.springframework.web.ErrorResponse springError) {

            ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_00007", "요청 처리 실패", e.getMessage());

            return new ResponseEntity<>(errorResponse, springError.getStatusCode());
        }

        e.printStackTrace();

        ErrorResponse errorResponse = new ErrorResponse("ERROR_CODE_99999", "서버 내부 오류", e.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
