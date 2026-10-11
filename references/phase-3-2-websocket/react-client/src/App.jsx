import { Navigate, Route, Routes } from 'react-router';

import ChatWidget from './components/ChatWidget.jsx';
import Header from './components/Header.jsx';
import RequireAdmin from './components/RequireAdmin.jsx';
import RequireAuth from './components/RequireAuth.jsx';
import CartPage from './pages/CartPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MenuDetailPage from './pages/MenuDetailPage.jsx';
import MenuListPage from './pages/MenuListPage.jsx';
import OrderListPage from './pages/OrderListPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import AdminMenuFormPage from './pages/admin/AdminMenuFormPage.jsx';
import AdminMenuListPage from './pages/admin/AdminMenuListPage.jsx';
import AdminOrderListPage from './pages/admin/AdminOrderListPage.jsx';
import styles from './App.module.css';

/*
 * 화면 주소(React)와 서버 주소(Spring)는 서로 다른 것이다.
 *   화면 주소 : 브라우저 주소창에 보이는 /menus/12        -> 어떤 컴포넌트를 그릴지 정한다
 *   서버 주소 : 화면이 데이터를 받으러 가는 /api/menus/12 -> api/ 폴더의 함수가 요청한다
 * 서버 주소를 모두 /api 로 시작하게 만든 것은 둘을 헷갈리지 않게 하기 위해서다.
 */
export default function App() {
    return (
        <>
            <Header />
            <main className={styles.main}>
                <Routes>
                    <Route path="/" element={<Navigate to="/menus" replace />} />
                    <Route path="/menus" element={<MenuListPage />} />
                    <Route path="/menus/:menuCode" element={<MenuDetailPage />} />
                    <Route path="/cart" element={<RequireAuth><CartPage /></RequireAuth>} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />

                    <Route path="/orders" element={<RequireAuth><OrderListPage /></RequireAuth>} />

                    <Route path="/admin/menus" element={<RequireAdmin><AdminMenuListPage /></RequireAdmin>} />
                    <Route path="/admin/menus/new" element={<RequireAdmin><AdminMenuFormPage key="new" /></RequireAdmin>} />
                    <Route
                        path="/admin/menus/:menuCode/edit"
                        element={<RequireAdmin><AdminMenuFormPage key="edit" /></RequireAdmin>}
                    />
                    <Route path="/admin/orders" element={<RequireAdmin><AdminOrderListPage /></RequireAdmin>} />

                    <Route path="*" element={<Navigate to="/menus" replace />} />
                </Routes>
            </main>

            {/* [Phase 3] AI 추천 창은 <Routes> 바깥에 둔다. 페이지를 옮겨도 사라지지 않아 대화가 이어진다. */}
            <ChatWidget />
        </>
    );
}
