package com.ohgiraffers.restaurant.menu.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Integer> {

    /* 목차. 1. 손님용 메뉴 목록 (주문 가능 + 삭제되지 않은 메뉴만) */
    /* 설명. 카테고리와 검색어는 선택 조건이다. (null이면 조건을 적용하지 않는다)
     *  ----------------------------------------------------------------------------------
     *  - 상위 카테고리(식사·음료·디저트)를 고르면 그 아래 하위 카테고리의 메뉴까지 함께 조회한다.
     *    그래서 메뉴의 카테고리(c) 또는 그 상위 카테고리(p)가 일치하는지 둘 다 확인한다.
     *  - 상위 카테고리가 없는 카테고리도 있으므로 p는 LEFT JOIN으로 연결한다.
     *    (그냥 JOIN이면 최상위 카테고리에 바로 속한 메뉴가 결과에서 빠진다)
     *  - JOIN FETCH m.category : 목록 응답에 카테고리명이 필요하므로 메뉴와 함께 한 번에 가져온다.
     *  ----------------------------------------------------------------------------------
     *  주의. 페치 조인이 들어간 쿼리는 전체 개수를 세는 countQuery를 따로 적어주는 편이 안전하다.
     * */
    @Query(value = """
            SELECT m FROM Menu m
              JOIN FETCH m.category c
              LEFT JOIN c.parentCategory p
             WHERE m.orderableStatus = 'Y'
               AND m.deletedStatus = 'N'
               AND (:categoryCode IS NULL OR c.categoryCode = :categoryCode OR p.categoryCode = :categoryCode)
               AND (:keyword IS NULL OR m.menuName LIKE CONCAT('%', :keyword, '%'))
            """,
            countQuery = """
            SELECT COUNT(m) FROM Menu m
              JOIN m.category c
              LEFT JOIN c.parentCategory p
             WHERE m.orderableStatus = 'Y'
               AND m.deletedStatus = 'N'
               AND (:categoryCode IS NULL OR c.categoryCode = :categoryCode OR p.categoryCode = :categoryCode)
               AND (:keyword IS NULL OR m.menuName LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Menu> searchOrderableMenus(@Param("categoryCode") Integer categoryCode,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    /* 목차. 2. 관리자용 메뉴 목록 (주문 불가 메뉴 포함, 삭제된 메뉴 제외) */
    /* 설명. 쿼리 메소드에도 @EntityGraph를 붙이면 지정한 연관 엔티티를 페치 조인으로 함께 가져온다. */
    @EntityGraph(attributePaths = "category")
    Page<Menu> findByDeletedStatus(String deletedStatus, Pageable pageable);

    /* 목차. 3. 메뉴 상세 (카테고리 + 알레르기까지 한 번에) */
    /* 설명. 상세 화면에는 카테고리명과 알레르기 목록이 모두 필요하다.
     *  findById()로 가져온 뒤 하나씩 꺼내면 카테고리 1번, 알레르기 목록 1번, 알레르기마다 1번씩
     *  SELECT가 추가로 실행되므로, 페치 조인으로 한 번의 쿼리에 모두 가져온다.
     * */
    @Query("""
            SELECT m FROM Menu m
              JOIN FETCH m.category
              LEFT JOIN FETCH m.menuAllergens ma
              LEFT JOIN FETCH ma.allergen
             WHERE m.menuCode = :menuCode
               AND m.deletedStatus = 'N'
            """)
    Optional<Menu> findDetailByMenuCode(@Param("menuCode") int menuCode);

    /* 목차. 4. [Phase 3] AI 추천 후보 메뉴 */
    /* 설명. AI 가 손님의 말에서 뽑은 조건(MenuFilter)으로 추천 후보를 고른다.
     *  ----------------------------------------------------------------------------------
     *  - 주문 가능 + 삭제되지 않은 메뉴만 고른다. (1번 손님용 목록과 같다)
     *  - 카테고리 이름과 최대 가격은 선택 조건이다. (null 이면 조건을 적용하지 않는다)
     *    상위 카테고리(식사)를 말하면 하위 카테고리(한식, 중식 ...)의 메뉴도 함께 고른다.
     *  - 빼야 하는 알레르기가 하나라도 들어 있는 메뉴는 NOT EXISTS 로 뺀다.
     *    excludeAllergens 가 빈 목록이면 'IN ()' 이 아무것도 고르지 않으므로 모든 메뉴가 남는다.
     *  - JOIN FETCH m.category : 이 쿼리는 요청 스레드가 아닌 다른 스레드에서 실행되어 지연 로딩을 쓸 수 없다.
     *    카테고리명이 필요하므로 처음부터 함께 가져온다. (RecommendationService 참고)
     *  ----------------------------------------------------------------------------------
     *  Pageable 은 개수 제한(LIMIT)용이다. 후보를 너무 많이 프롬프트에 넣으면 입력 토큰(비용)이 늘어난다.
     * */
    @Query("""
            SELECT m FROM Menu m
              JOIN FETCH m.category c
              LEFT JOIN c.parentCategory p
             WHERE m.orderableStatus = 'Y'
               AND m.deletedStatus = 'N'
               AND (:categoryName IS NULL OR c.categoryName = :categoryName OR p.categoryName = :categoryName)
               AND (:maxPrice IS NULL OR m.menuPrice <= :maxPrice)
               AND NOT EXISTS (
                     SELECT ma FROM MenuAllergen ma
                      WHERE ma.menu = m
                        AND ma.allergen.allergenName IN :excludeAllergens)
             ORDER BY m.menuCode
            """)
    List<Menu> findRecommendationCandidates(@Param("categoryName") String categoryName,
                                            @Param("maxPrice") Integer maxPrice,
                                            @Param("excludeAllergens") List<String> excludeAllergens,
                                            Pageable pageable);
}
