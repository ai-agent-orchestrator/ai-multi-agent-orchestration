import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';

import { deleteMenu, fetchAdminMenus } from '../../api/adminApi.js';
import Button from '../../components/Button.jsx';
import MenuImage from '../../components/MenuImage.jsx';
import Pagination from '../../components/Pagination.jsx';
import StatusBlock from '../../components/StatusBlock.jsx';
import { formatPrice } from '../../format.js';
import styles from './AdminMenuListPage.module.css';

const PAGE_SIZE = 10;

export default function AdminMenuListPage() {
    const [params, setParams] = useSearchParams();
    const page = Math.max(1, Number(params.get('page')) || 1);
    const navigate = useNavigate();

    const [menuPage, setMenuPage] = useState(null);
    const [error, setError] = useState(null);
    // 삭제 후 목록을 다시 받기 위한 값. 이 숫자가 바뀌면 아래 effect 가 다시 실행된다.
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            setError(null);
            try {
                setMenuPage(await fetchAdminMenus({ page, size: PAGE_SIZE }, controller.signal));
            } catch (err) {
                if (controller.signal.aborted) return;
                setError(err.message);
            }
        }

        load();
        return () => controller.abort();
    }, [page, reloadKey]);

    const goPage = useCallback((n) => setParams(n > 1 ? { page: String(n) } : {}), [setParams]);

    async function handleDelete(menu) {
        // 바로 지우지 않고 한 번 더 확인을 받는다.
        if (!window.confirm(`'${menu.menuName}' 메뉴를 삭제할까요?\n삭제한 메뉴는 손님 화면과 관리자 목록에서 사라집니다.`)) {
            return;
        }
        try {
            await deleteMenu(menu.menuCode);
            setReloadKey((key) => key + 1);
        } catch (err) {
            window.alert(err.message);
        }
    }

    return (
        <div className={styles.page}>
            <div className={styles.head}>
                <div>
                    <h1 className="title2">메뉴 관리</h1>
                    <p className={`${styles.summary} label1`}>
                        {menuPage ? `등록된 메뉴 ${menuPage.totalElements}개 (주문 불가 메뉴 포함)` : error ? '' : '불러오는 중…'}
                    </p>
                </div>
                <Button onClick={() => navigate('/admin/menus/new')}>메뉴 등록</Button>
            </div>

            {error ? (
                <StatusBlock tone="error" title="메뉴를 불러오지 못했습니다" description={error} />
            ) : menuPage && (
                <>
                    <div className={styles.tableWrap}>
                        <table className={styles.table}>
                            <thead>
                                <tr className="label2">
                                    <th scope="col">이미지</th>
                                    <th scope="col">메뉴</th>
                                    <th scope="col">카테고리</th>
                                    <th scope="col">가격</th>
                                    <th scope="col">주문 가능</th>
                                    <th scope="col"><span className={styles.srOnly}>관리</span></th>
                                </tr>
                            </thead>
                            <tbody className="body2">
                                {menuPage.content.map((menu) => (
                                    <tr key={menu.menuCode}>
                                        <td>
                                            <div className={styles.thumb}>
                                                <MenuImage path={menu.menuImageUrl} compact />
                                            </div>
                                        </td>
                                        <td>
                                            <Link to={`/menus/${menu.menuCode}`} className={styles.name}>
                                                {menu.menuName}
                                            </Link>
                                        </td>
                                        <td>{menu.categoryName}</td>
                                        <td>{formatPrice(menu.menuPrice)}</td>
                                        <td>
                                            <span className={`${styles.status} ${menu.orderableStatus === 'Y' ? styles.on : styles.off} label2`}>
                                                {menu.orderableStatus === 'Y' ? '가능' : '불가'}
                                            </span>
                                        </td>
                                        <td>
                                            <div className={styles.actions}>
                                                <Button
                                                    size="small"
                                                    variant="secondary"
                                                    onClick={() => navigate(`/admin/menus/${menu.menuCode}/edit`)}
                                                >
                                                    수정
                                                </Button>
                                                <Button size="small" variant="secondary" onClick={() => handleDelete(menu)}>
                                                    삭제
                                                </Button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <Pagination page={menuPage.number} lastPage={menuPage.totalPages} onChange={goPage} />
                </>
            )}
        </div>
    );
}
