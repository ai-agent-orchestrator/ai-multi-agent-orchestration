package com.ohgiraffers.restaurant.menu.service;

import com.ohgiraffers.restaurant.common.exception.NotFoundException;
import com.ohgiraffers.restaurant.common.file.FileStorage;
import com.ohgiraffers.restaurant.menu.domain.*;
import com.ohgiraffers.restaurant.menu.dto.MenuDetailResponse;
import com.ohgiraffers.restaurant.menu.dto.MenuRequest;
import com.ohgiraffers.restaurant.menu.dto.MenuResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;

@Service
public class MenuService {

    /* 설명. 메뉴 이미지가 저장될 하위 폴더명 (예: C:/myWs/uploadedFiles/menus) */
    private static final String MENU_IMAGE_DIR = "menus";

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final AllergenRepository allergenRepository;
    private final FileStorage fileStorage;

    public MenuService(MenuRepository menuRepository,
                       CategoryRepository categoryRepository,
                       AllergenRepository allergenRepository,
                       FileStorage fileStorage) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.allergenRepository = allergenRepository;
        this.fileStorage = fileStorage;
    }

    /* 목차. 1. 손님용 메뉴 목록 조회 (주문 가능한 메뉴만) */
    /**
     * 주문 가능한 메뉴를 페이지 단위로 조회한다.
     *
     * @param categoryCode 카테고리 코드 (선택, 상위 카테고리를 넘기면 하위 카테고리까지 포함)
     * @param keyword 메뉴명 검색어 (선택)
     * @param pageable 페이지 번호와 크기
     * @return 페이징 처리된 메뉴 목록
     */
    @Transactional(readOnly = true)
    public Page<MenuResponse> findOrderableMenus(Integer categoryCode, String keyword, Pageable pageable) {

        // 빈 문자열 검색어는 '검색하지 않음'으로 취급한다.
        String searchKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        // 손님 화면은 메뉴판처럼 등록 순서(메뉴 코드 오름차순)로 보여준다.
        Pageable menuBoardOrder = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("menuCode"));

        Page<Menu> menuPage = menuRepository.searchOrderableMenus(categoryCode, searchKeyword, menuBoardOrder);

        return menuPage.map(MenuResponse::from);
    }

    /* 목차. 2. 관리자용 메뉴 목록 조회 (주문 불가 메뉴 포함) */
    @Transactional(readOnly = true)
    public Page<MenuResponse> findMenusForAdmin(Pageable pageable) {

        Page<Menu> menuPage = menuRepository.findByDeletedStatus("N", latestFirst(pageable));

        return menuPage.map(MenuResponse::from);
    }

    /* 목차. 3. 메뉴 상세 조회 */
    @Transactional(readOnly = true)
    public MenuDetailResponse findMenuDetail(int menuCode) {

        return MenuDetailResponse.from(findMenuDetailOrThrow(menuCode));
    }

    /* 목차. 4. 메뉴 등록 (관리자) */
    /**
     * 새 메뉴를 등록한다. 이미지는 선택 사항이다.
     *
     * <p>이미지 파일을 먼저 외부 폴더에 저장하고, 저장된 파일명을 메뉴와 함께 DB에 기록한다.
     * DB 저장 중 예외가 발생하면 방금 저장한 파일을 지워서 주인 없는 파일이 남지 않게 한다.</p>
     *
     * @param menuRequest 등록할 메뉴 정보
     * @param menuImage 메뉴 이미지 (선택)
     * @return 등록된 메뉴 상세 정보
     */
    @Transactional
    public MenuDetailResponse registMenu(MenuRequest menuRequest, MultipartFile menuImage) {

        Category category = findCategoryOrThrow(menuRequest.getCategoryCode());
        List<Allergen> allergens = findAllergensOrThrow(menuRequest.getAllergenCodes());

        String savedImageName = saveImageIfPresent(menuImage);

        try {
            Menu menu = new Menu(
                    menuRequest.getMenuName(),
                    menuRequest.getMenuPrice(),
                    category,
                    menuRequest.getOrderableStatus(),
                    menuRequest.getMenuDescription(),
                    menuRequest.getMenuIngredients(),
                    savedImageName
            );
            menu.changeAllergens(allergens);

            // cascade 설정 덕분에 메뉴를 저장하면 메뉴별 알레르기도 함께 INSERT 된다.
            Menu savedMenu = menuRepository.save(menu);

            return MenuDetailResponse.from(savedMenu);

        } catch (RuntimeException e) {
            fileStorage.delete(savedImageName, MENU_IMAGE_DIR);
            throw e;
        }
    }

    /* 목차. 5. 메뉴 수정 (관리자) */
    /**
     * 메뉴 정보를 수정한다. 새 이미지를 보낸 경우에만 이미지를 교체한다.
     *
     * <p>조회해온 엔티티는 영속 상태이므로 값만 바꿔두면
     * 트랜잭션이 끝날 때 변경 감지(dirty checking)로 UPDATE가 실행된다.</p>
     *
     * @param menuCode 수정할 메뉴 코드
     * @param menuRequest 수정할 메뉴 정보
     * @param menuImage 새 메뉴 이미지 (선택)
     * @return 수정된 메뉴 상세 정보
     */
    @Transactional
    public MenuDetailResponse modifyMenu(int menuCode, MenuRequest menuRequest, MultipartFile menuImage) {

        Menu menu = findMenuDetailOrThrow(menuCode);
        Category category = findCategoryOrThrow(menuRequest.getCategoryCode());
        List<Allergen> allergens = findAllergensOrThrow(menuRequest.getAllergenCodes());

        menu.modify(
                menuRequest.getMenuName(),
                menuRequest.getMenuPrice(),
                category,
                menuRequest.getOrderableStatus(),
                menuRequest.getMenuDescription(),
                menuRequest.getMenuIngredients()
        );
        menu.changeAllergens(allergens);

        String newImageName = saveImageIfPresent(menuImage);
        if (newImageName != null) {
            String oldImageName = menu.getMenuImageName();
            menu.changeImage(newImageName);
            fileStorage.delete(oldImageName, MENU_IMAGE_DIR);
        }

        return MenuDetailResponse.from(menu);
    }

    /* 목차. 6. 메뉴 삭제 (관리자, soft delete) */
    /**
     * 메뉴를 삭제 상태로 바꾼다. DB의 행과 이미지 파일은 지우지 않는다.
     *
     * <p>클라이언트는 DELETE 메서드로 요청하지만, 서버는 실제로 행을 지우는 대신
     * 삭제 여부(deleted_status)만 'Y'로 바꾼다. 지난 주문 내역이 이 메뉴를 참조하고 있기 때문이다.
     * 이미지 파일도 지난 주문 내역 화면에서 쓸 수 있으므로 남겨둔다.</p>
     *
     * @param menuCode 삭제할 메뉴 코드
     */
    @Transactional
    public void removeMenu(int menuCode) {

        Menu menu = menuRepository.findById(menuCode)
                .filter(foundMenu -> !foundMenu.isDeleted())
                .orElseThrow(() -> new NotFoundException("해당 메뉴가 존재하지 않습니다. menuCode=" + menuCode));

        menu.delete();
    }

    /* 목차. 0. 공통으로 사용하는 메서드 */

    /* 설명. 관리자 목록은 방금 등록한 메뉴를 바로 확인할 수 있도록 최근에 등록한 메뉴부터 보여준다.
     *  페이지 번호와 크기는 요청 값을 그대로 쓰고, 정렬 기준만 정한다.
     * */
    private Pageable latestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("menuCode").descending());
    }

    private Menu findMenuDetailOrThrow(int menuCode) {
        return menuRepository.findDetailByMenuCode(menuCode)
                .orElseThrow(() -> new NotFoundException("해당 메뉴가 존재하지 않습니다. menuCode=" + menuCode));
    }

    /* 설명. 존재하지 않는 카테고리 코드는 '메뉴가 없는 것'이 아니라 '요청 값이 잘못된 것'이므로 400으로 응답한다. */
    private Category findCategoryOrThrow(int categoryCode) {
        return categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리 코드입니다: " + categoryCode));
    }

    private List<Allergen> findAllergensOrThrow(List<Integer> allergenCodes) {

        // 같은 코드가 두 번 들어온 경우를 대비해 중복을 제거한다.
        List<Integer> distinctCodes = new HashSet<>(allergenCodes).stream().toList();
        List<Allergen> allergens = allergenRepository.findAllById(distinctCodes);

        if (allergens.size() != distinctCodes.size()) {
            throw new IllegalArgumentException("유효하지 않은 알레르기 코드가 포함되어 있습니다: " + allergenCodes);
        }

        return allergens;
    }

    /* 설명. 이미지를 보낸 경우에만 저장하고 저장된 파일명을 반환한다. 이미지가 없으면 null이다. */
    private String saveImageIfPresent(MultipartFile menuImage) {

        if (menuImage == null || menuImage.isEmpty()) {
            return null;
        }

        String contentType = menuImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다: " + menuImage.getOriginalFilename());
        }

        return fileStorage.save(menuImage, MENU_IMAGE_DIR);
    }
}
