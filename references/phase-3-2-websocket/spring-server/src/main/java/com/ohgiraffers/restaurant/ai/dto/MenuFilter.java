package com.ohgiraffers.restaurant.ai.dto;

import java.util.List;

/* 설명. 손님이 보낸 채팅에서 AI가 추출한 메뉴 검색 조건 (구조화 출력의 결과)
 *  OpenAI 에게 이 모양의 JSON 으로만 응답하라고 JSON Schema 를 함께 보낸다. (RecommendationService.filterSchema)
 *  - categoryName     : 카테고리 이름 (예: 식사, 커피). 말하지 않았으면 null
 *  - maxPrice         : 최대 가격(원). 말하지 않았으면 null
 *  - excludeAllergens : 빼야 하는 알레르기 이름 (예: 땅콩)
 *  - keywords         : 맛 · 분위기 같은 나머지 바람 (예: 매콤, 따뜻한). DB 검색에는 쓰지 않고 추천 문장을 만들 때 쓴다
 * */
public class MenuFilter {

    private String categoryName;
    private Integer maxPrice;
    private List<String> excludeAllergens = List.of();
    private List<String> keywords = List.of();

    public MenuFilter() {}

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getExcludeAllergens() {
        return excludeAllergens;
    }

    public void setExcludeAllergens(List<String> excludeAllergens) {
        this.excludeAllergens = excludeAllergens;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "MenuFilter{categoryName=" + categoryName + ", maxPrice=" + maxPrice
                + ", excludeAllergens=" + excludeAllergens + ", keywords=" + keywords + "}";
    }
}
