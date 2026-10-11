package com.ohgiraffers.restaurant.menu.service;

import com.ohgiraffers.restaurant.menu.domain.AllergenRepository;
import com.ohgiraffers.restaurant.menu.dto.AllergenResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllergenService {

    private final AllergenRepository allergenRepository;

    public AllergenService(AllergenRepository allergenRepository) {
        this.allergenRepository = allergenRepository;
    }

    /* 목차. 1. 전체 알레르기 조회 (관리자 메뉴 등록 화면의 체크박스 목록용) */
    @Transactional(readOnly = true)
    public List<AllergenResponse> findAllAllergens() {

        return allergenRepository.findAll(Sort.by("allergenCode")).stream()
                .map(AllergenResponse::from)
                .toList();
    }
}
