package com.ohgiraffers.restaurant.menu.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_menu")
public class Menu {

    @Id
    @Column(name = "menu_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int menuCode;

    @Column(name = "menu_name")
    private String menuName;

    @Column(name = "menu_price")
    private int menuPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code")
    private Category category;

    /* 설명. 'Y'면 지금 주문할 수 있고, 'N'이면 매진·재료 소진·시즌 종료 등으로 주문할 수 없다. */
    @Column(name = "orderable_status")
    private String orderableStatus;

    @Column(name = "menu_description")
    private String menuDescription;

    @Column(name = "menu_ingredients")
    private String menuIngredients;

    /* 설명. 저장된 이미지 '파일명'만 가진다. 파일이 있는 폴더는 FileStorage가 알고 있다. */
    @Column(name = "menu_image_name")
    private String menuImageName;

    /* 설명. 'Y'면 관리자가 삭제한 메뉴다. (soft delete, 실제 행은 지우지 않는다) */
    @Column(name = "deleted_status")
    private String deletedStatus;

    /* 설명. 메뉴에 들어간 알레르기 목록 (1:N)
     *  ----------------------------------------------------------------------------------
     *  - mappedBy = "menu"   : 외래키(menu_code)는 MenuAllergen 쪽이 관리한다는 의미다.
     *  - cascade = ALL       : 메뉴를 저장하면 이 목록에 담긴 MenuAllergen도 함께 저장된다.
     *  - orphanRemoval = true: 이 목록에서 빠진 MenuAllergen은 DB에서도 삭제된다.
     *  덕분에 메뉴별 알레르기를 따로 저장·삭제하지 않고, 메뉴의 목록만 바꾸면 된다.
     * */
    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuAllergen> menuAllergens = new ArrayList<>();

    public Menu() {}

    public Menu(String menuName, int menuPrice, Category category, String orderableStatus,
                String menuDescription, String menuIngredients, String menuImageName) {
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.category = category;
        this.orderableStatus = orderableStatus;
        this.menuDescription = menuDescription;
        this.menuIngredients = menuIngredients;
        this.menuImageName = menuImageName;
        this.deletedStatus = "N";
    }

    /* 목차. 메뉴 정보 수정 (이미지와 알레르기는 따로 수정한다) */
    public void modify(String menuName, int menuPrice, Category category, String orderableStatus,
                       String menuDescription, String menuIngredients) {
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.category = category;
        this.orderableStatus = orderableStatus;
        this.menuDescription = menuDescription;
        this.menuIngredients = menuIngredients;
    }

    /* 목차. 이미지 교체 */
    public void changeImage(String menuImageName) {
        this.menuImageName = menuImageName;
    }

    /* 목차. 알레르기 목록 교체 */
    /* 설명. 기존 목록을 전부 지우고(clear) 새로 담으면 안 되는 이유
     *  ----------------------------------------------------------------------------------
     *  Hibernate는 트랜잭션이 끝날 때 쌓아둔 SQL을 INSERT -> ... -> DELETE 순서로 실행한다.
     *  예를 들어 '우유'를 지웠다가 다시 담으면, 기존 (메뉴, 우유) 행이 DELETE 되기도 전에
     *  새 (메뉴, 우유) 행이 먼저 INSERT 되어 UNIQUE 제약 조건(uk_menu_allergen) 위반이 발생한다.
     *  ----------------------------------------------------------------------------------
     *  그래서 '빠진 것만 제거하고, 새로 생긴 것만 추가'하는 방식으로 바꾼다.
     * */
    public void changeAllergens(List<Allergen> newAllergens) {

        Set<Integer> newCodes = newAllergens.stream()
                .map(Allergen::getAllergenCode)
                .collect(Collectors.toSet());

        // 1. 새 목록에 없는 알레르기는 제거한다. (orphanRemoval로 DELETE 된다)
        menuAllergens.removeIf(menuAllergen -> !newCodes.contains(menuAllergen.getAllergen().getAllergenCode()));

        // 2. 기존 목록에 없던 알레르기만 추가한다. (cascade로 INSERT 된다)
        Set<Integer> currentCodes = menuAllergens.stream()
                .map(menuAllergen -> menuAllergen.getAllergen().getAllergenCode())
                .collect(Collectors.toSet());

        for (Allergen allergen : newAllergens) {
            if (!currentCodes.contains(allergen.getAllergenCode())) {
                menuAllergens.add(new MenuAllergen(this, allergen));
            }
        }
    }

    /* 목차. 메뉴 삭제 (soft delete) */
    /* 설명. 행을 지우지 않고 삭제 여부만 'Y'로 표시한다.
     *  지난 주문 내역(tbl_order_menu)이 이 메뉴를 참조하고 있으므로 실제로 지우면 안 된다.
     *  삭제된 메뉴는 더 이상 주문할 수 없도록 주문 가능 상태도 'N'으로 바꾼다.
     * */
    public void delete() {
        this.deletedStatus = "Y";
        this.orderableStatus = "N";
    }

    public boolean isOrderable() {
        return "Y".equals(orderableStatus) && "N".equals(deletedStatus);
    }

    public boolean isDeleted() {
        return "Y".equals(deletedStatus);
    }

    public int getMenuCode() {
        return menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getMenuPrice() {
        return menuPrice;
    }

    public Category getCategory() {
        return category;
    }

    public String getOrderableStatus() {
        return orderableStatus;
    }

    public String getMenuDescription() {
        return menuDescription;
    }

    public String getMenuIngredients() {
        return menuIngredients;
    }

    public String getMenuImageName() {
        return menuImageName;
    }

    public String getDeletedStatus() {
        return deletedStatus;
    }

    public List<MenuAllergen> getMenuAllergens() {
        return menuAllergens;
    }

    /* 설명. 연관 엔티티(category, menuAllergens)는 toString()에 넣지 않는다.
     *  지연 로딩 대상을 출력하려는 순간 추가 SELECT가 실행되거나, 양방향 참조에서 무한 호출이 일어날 수 있다.
     * */
    @Override
    public String toString() {
        return "Menu{" +
                "menuCode=" + menuCode +
                ", menuName='" + menuName + '\'' +
                ", menuPrice=" + menuPrice +
                ", orderableStatus='" + orderableStatus + '\'' +
                ", deletedStatus='" + deletedStatus + '\'' +
                '}';
    }
}
