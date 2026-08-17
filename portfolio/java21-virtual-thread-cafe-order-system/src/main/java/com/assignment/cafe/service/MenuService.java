package com.assignment.cafe.service;

import com.assignment.cafe.model.CafeMenu;
import com.assignment.cafe.model.MenuCategory;

import java.util.List;
import java.util.Optional;

public class MenuService {
    private final List<CafeMenu> menus = List.of(
            new CafeMenu(1, "아메리카노", MenuCategory.COFFEE, 3000),
            new CafeMenu(2, "카페라떼", MenuCategory.COFFEE, 4200),
            new CafeMenu(3, "바닐라라떼", MenuCategory.COFFEE, 4800),
            new CafeMenu(4, "캐모마일", MenuCategory.TEA, 3900),
            new CafeMenu(5, "레몬에이드", MenuCategory.ADE, 5200),
            new CafeMenu(6, "자몽에이드", MenuCategory.ADE, 5400),
            new CafeMenu(7, "치즈케이크", MenuCategory.DESSERT, 6200),
            new CafeMenu(8, "초코쿠키", MenuCategory.DESSERT, 2800)
    );

    public List<CafeMenu> findAll() {
        return menus;
    }

    public CafeMenu findById(int id) {
        Optional<CafeMenu> foundMenu = menus.stream()
                .filter(menu -> menu.getId() == id)
                .findFirst();

        return foundMenu.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴 번호입니다."));
    }
}
