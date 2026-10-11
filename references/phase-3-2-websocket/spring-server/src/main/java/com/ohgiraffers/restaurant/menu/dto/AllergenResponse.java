package com.ohgiraffers.restaurant.menu.dto;

import com.ohgiraffers.restaurant.menu.domain.Allergen;

/* 설명. 알레르기 응답 DTO */
public class AllergenResponse {

    private int allergenCode;
    private String allergenName;

    public AllergenResponse() {}

    public AllergenResponse(int allergenCode, String allergenName) {
        this.allergenCode = allergenCode;
        this.allergenName = allergenName;
    }

    public static AllergenResponse from(Allergen allergen) {
        return new AllergenResponse(allergen.getAllergenCode(), allergen.getAllergenName());
    }

    public int getAllergenCode() {
        return allergenCode;
    }

    public String getAllergenName() {
        return allergenName;
    }
}
