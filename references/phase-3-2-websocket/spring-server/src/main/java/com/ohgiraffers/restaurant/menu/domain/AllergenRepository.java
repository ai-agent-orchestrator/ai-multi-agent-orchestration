package com.ohgiraffers.restaurant.menu.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergenRepository extends JpaRepository<Allergen, Integer> {
}
