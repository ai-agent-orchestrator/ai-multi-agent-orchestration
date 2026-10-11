import { api, resultOf } from './client.js';

/*
 * 관리자 전용 API. 서버는 모두 /api/admin 아래에 두었다.
 * [Phase 2] 서버가 토큰의 권한을 보고 막는다. 토큰이 없으면 401, 관리자가 아니면 403 이 돌아온다.
 */

// AdminMenuController.findMenusForAdmin()   GET /api/admin/menus?page=&size=
export async function fetchAdminMenus({ page = 1, size = 10 }, signal) {
    const response = await api.get('/api/admin/menus', { params: { page, size }, signal });
    return resultOf(response).menuPage;
}

/*
 * AdminMenuController.registMenu()   POST /api/admin/menus   (@ModelAttribute MenuRequest + MultipartFile menuImage)
 *
 * 이미지 파일을 함께 보내야 하므로 JSON 이 아니라 FormData(multipart/form-data)로 보낸다.
 * FormData 를 넘기면 axios 가 Content-Type 과 경계(boundary) 문자열을 알아서 붙인다.
 * Content-Type 을 손으로 'multipart/form-data' 라고 적으면 boundary 가 빠져서 서버가 읽지 못한다.
 */
export async function createMenu(formData) {
    const response = await api.post('/api/admin/menus', formData);
    return resultOf(response).menu;
}

// AdminMenuController.modifyMenu()   PUT /api/admin/menus/{menuCode}   (@PathVariable + @ModelAttribute + MultipartFile)
export async function updateMenu(menuCode, formData) {
    const response = await api.put(`/api/admin/menus/${menuCode}`, formData);
    return resultOf(response).menu;
}

// AdminMenuController.removeMenu()   DELETE /api/admin/menus/{menuCode}
// 서버는 행을 지우지 않고 삭제 표시만 한다(soft delete). 응답 상태는 204 가 아니라 200 이다.
export async function deleteMenu(menuCode) {
    const response = await api.delete(`/api/admin/menus/${menuCode}`);
    return resultOf(response).deletedMenuCode;
}

// AdminOrderController.findAllOrders()   GET /api/admin/orders
export async function fetchAllOrders(signal) {
    const response = await api.get('/api/admin/orders', { signal });
    return resultOf(response).orders;
}
