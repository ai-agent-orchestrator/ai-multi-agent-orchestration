import { api, resultOf } from './client.js';

// CategoryController.findAllCategories()   GET /api/categories
export async function fetchCategories() {
    const response = await api.get('/api/categories');
    return resultOf(response).categories;
}

// AllergenController.findAllAllergens()   GET /api/allergens
export async function fetchAllergens() {
    const response = await api.get('/api/allergens');
    return resultOf(response).allergens;
}

/*
 * MenuController.findMenus()   GET /api/menus?page=&size=&categoryCode=&keyword=
 * (@RequestParam categoryCode, keyword + Pageable page, size)
 *
 * axios 의 params 에 객체를 넘기면 쿼리스트링으로 바꿔 붙여준다.
 * 값이 undefined 인 항목은 아예 빠지므로, 선택 조건은 없으면 undefined 로 넘긴다.
 * 페이지 번호는 1부터 센다. (서버의 one-indexed-parameters 설정)
 */
export async function fetchMenus({ page = 1, size = 8, categoryCode, keyword }, signal) {
    const response = await api.get('/api/menus', {
        params: {
            page,
            size,
            categoryCode: categoryCode || undefined,
            keyword: keyword || undefined,
        },
        signal,
    });
    return resultOf(response).menuPage;
}

// MenuController.findMenuDetail()   GET /api/menus/{menuCode}   (@PathVariable)
export async function fetchMenu(menuCode, signal) {
    const response = await api.get(`/api/menus/${menuCode}`, { signal });
    return resultOf(response).menu;
}
