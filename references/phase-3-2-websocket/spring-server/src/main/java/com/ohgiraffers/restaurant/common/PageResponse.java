package com.ohgiraffers.restaurant.common;

import org.springframework.data.domain.Page;

import java.util.List;

/* 설명. 페이징 결과를 화면에서 쓰기 좋은 모양으로 옮겨 담는 클래스
 *  chap06에서는 컨트롤러마다 Page의 값을 Map에 하나씩 꺼내 담았다.
 *  페이징 API가 여러 개로 늘어나면 같은 코드가 반복되므로 한 곳으로 모았다.
 *  ----------------------------------------------------------------------------------
 *  주의. application.yaml의 one-indexed-parameters 설정은 '요청'의 page 파라미터에만 적용된다.
 *       Page.getNumber()는 여전히 0부터 시작하므로, 요청과 기준을 맞추기 위해 +1 해서 내려준다.
 * */
public class PageResponse<T> {

    private List<T> content;          // 현재 페이지의 데이터
    private long totalElements;       // 전체 데이터 수
    private int totalPages;           // 전체 페이지 수
    private int size;                 // 페이지 크기
    private int number;               // 현재 페이지 번호(1부터 시작)
    private boolean first;            // 첫 페이지 여부
    private boolean last;             // 마지막 페이지 여부

    public PageResponse() {}

    public static <T> PageResponse<T> from(Page<T> page) {

        PageResponse<T> response = new PageResponse<>();
        response.content = page.getContent();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.size = page.getSize();
        response.number = page.getNumber() + 1;
        response.first = page.isFirst();
        response.last = page.isLast();

        return response;
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getSize() {
        return size;
    }

    public int getNumber() {
        return number;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
