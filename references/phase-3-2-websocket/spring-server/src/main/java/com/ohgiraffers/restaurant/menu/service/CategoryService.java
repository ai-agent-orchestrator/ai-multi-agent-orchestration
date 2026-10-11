package com.ohgiraffers.restaurant.menu.service;

import com.ohgiraffers.restaurant.menu.domain.CategoryRepository;
import com.ohgiraffers.restaurant.menu.dto.CategoryResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /* 목차. 1. 전체 카테고리 조회 */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCategories() {

        return categoryRepository.findAll(Sort.by("categoryCode")).stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
