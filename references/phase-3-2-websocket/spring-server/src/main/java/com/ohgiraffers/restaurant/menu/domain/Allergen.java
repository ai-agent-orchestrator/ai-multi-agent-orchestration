package com.ohgiraffers.restaurant.menu.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_allergen")
public class Allergen {

    @Id
    @Column(name = "allergen_code")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int allergenCode;

    @Column(name = "allergen_name")
    private String allergenName;

    public Allergen() {}

    public int getAllergenCode() {
        return allergenCode;
    }

    public String getAllergenName() {
        return allergenName;
    }

    @Override
    public String toString() {
        return "Allergen{" +
                "allergenCode=" + allergenCode +
                ", allergenName='" + allergenName + '\'' +
                '}';
    }
}
