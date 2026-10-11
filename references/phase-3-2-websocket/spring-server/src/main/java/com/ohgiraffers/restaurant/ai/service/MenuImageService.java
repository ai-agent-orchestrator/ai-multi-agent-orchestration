package com.ohgiraffers.restaurant.ai.service;

import com.ohgiraffers.restaurant.ai.client.OpenAiClient;
import com.ohgiraffers.restaurant.ai.dto.MenuDraftMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/* 설명. [Phase 3-2 선택] 관리자용 메뉴 사진 만들기
 *  ----------------------------------------------------------------------------------
 *  메뉴 정보로 프롬프트를 만들어 이미지 모델에게 음식 사진 한 장을 부탁한다.
 *  완성된 이미지는 파일로 저장하지 않고 Base64 문자열 그대로 관리자 화면에 보낸다.
 *  화면은 이것을 '고른 이미지 파일'처럼 다루므로, 기존의 메뉴 등록, 수정(이미지 업로드)으로 그대로 저장된다.
 *  ----------------------------------------------------------------------------------
 *  주의. 이미지 한 장은 추천 한 번보다 수십 배 비싸다. (low 품질 기준 10원 안팎) 버튼을 누를 때만 만든다.
 * */
@Service
public class MenuImageService {

    // OpenAI 를 호출하는 부품 (이미지는 streamImage() 를 쓴다)
    private final OpenAiClient openAiClient;
    // application.yaml 의 openai.model.image, openai.image.* 값
    private final String model;
    private final String size;
    private final String quality;

    public MenuImageService(OpenAiClient openAiClient,
                            @Value("${openai.model.image}") String model,
                            @Value("${openai.image.size}") String size,
                            @Value("${openai.image.quality}") String quality) {
        this.openAiClient = openAiClient;
        this.model = model;
        this.size = size;
        this.quality = quality;
    }

    /**
     * 메뉴 사진 한 장을 만들어 Base64(jpeg) 로 돌려준다.
     *
     * @param onPartial 중간 미리보기(Base64)가 올 때마다 호출할 함수
     */
    public String generate(MenuDraftMessage menu, Consumer<String> onPartial) {

        // 이미지 모델은 영어, 한국어 모두 알아듣는다. 사진의 분위기를 정해 두어 메뉴판 사진들이 비슷한 느낌이 되게 한다.
        // 메뉴마다 바뀌는 부분(%s)만 채우고, 접시, 조명, 구도, 배경은 고정한다.
        String prompt = """
                레스토랑 메뉴판에 쓸 음식 사진 한 장.
                메뉴 : %s (%s)
                재료 : %s
                설명 : %s
                하얀 접시에 담긴 모습, 부드러운 자연광, 45도 위에서 찍은 구도, 배경은 단순하게.
                사진 안에 글자 · 로고 · 사람은 넣지 않는다.""".formatted(
                // 첫 번째 %s : 메뉴 이름
                menu.getMenuName(),
                // 두 번째 %s : 카테고리. [보조 메서드] orNone() : 비어 있으면 '정하지 않음'
                orNone(menu.getCategoryName()),
                // 세 번째 %s : 재료
                orNone(menu.getMenuIngredients()),
                // 네 번째 %s : 메뉴 설명 (초안을 먼저 만들어 두면 사진에도 반영된다)
                orNone(menu.getMenuDescription()));

        System.out.println("[MenuImageService] 사진 요청 : " + menu.getMenuName());
        // [Client 호출] streamImage() : Images API 로 사진 한 장을 만든다. 미리보기가 올 때마다 onPartial 을 호출하고,
        // 완성되면 Base64 문자열을 돌려준다. onPartial 은 핸들러가 넘겨준 함수다. (미리보기를 화면에 보낸다)
        return openAiClient.streamImage(model, prompt, size, quality, onPartial);
    }

    /* 설명. 비어 있는 값은 '정하지 않음'으로 바꾼다. (관리자가 아직 입력하지 않은 칸) */
    private String orNone(String value) {
        return value == null || value.isBlank() ? "정하지 않음" : value;
    }
}
