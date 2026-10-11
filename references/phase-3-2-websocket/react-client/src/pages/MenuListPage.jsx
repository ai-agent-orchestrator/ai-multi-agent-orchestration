import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';

import { fetchCategories, fetchMenus } from '../api/menuApi.js';
import Button from '../components/Button.jsx';
import CategoryTabs from '../components/CategoryTabs.jsx';
import MenuCard, { MenuCardSkeleton } from '../components/MenuCard.jsx';
import Pagination from '../components/Pagination.jsx';
import SearchField from '../components/SearchField.jsx';
import StatusBlock from '../components/StatusBlock.jsx';
import styles from './MenuListPage.module.css';

const PAGE_SIZE = 8;

export default function MenuListPage() {
    /*
     * 페이지·카테고리·검색어는 컴포넌트 state 가 아니라 URL 쿼리스트링에 둔다.
     * 새로고침해도 조건이 남고, 뒤로가기가 동작하고, 링크로 공유된다.
     *   /menus?category=2&keyword=스무디&page=2
     */
    const [params, setParams] = useSearchParams();
    const page = Math.max(1, Number(params.get('page')) || 1);
    const category = params.get('category') ? Number(params.get('category')) : null;
    const keyword = params.get('keyword') ?? '';

    const [categories, setCategories] = useState([]);
    const [menuPage, setMenuPage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // ── 카테고리 목록은 한 번만 받는다.
    useEffect(() => {
        let alive = true;
        fetchCategories()
            .then((list) => alive && setCategories(list))
            .catch((err) => alive && setError(err.message));
        return () => {
            alive = false;
        };
    }, []);

    // ── 조건(페이지·카테고리·검색어)이 바뀔 때마다 메뉴 목록을 다시 받는다.
    //    서버가 조건에 맞게 걸러서 한 페이지만 보내주므로, 화면은 받은 것을 그리기만 한다.
    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            setLoading(true);
            setError(null);
            try {
                const result = await fetchMenus(
                    { page, size: PAGE_SIZE, categoryCode: category, keyword },
                    controller.signal,
                );
                setMenuPage(result);
            } catch (err) {
                // 조건을 빠르게 바꾸면 이전 요청을 취소한다. 취소는 오류가 아니다.
                if (controller.signal.aborted) return;
                setError(err.message);
            }
            setLoading(false);
        }

        load();
        return () => controller.abort();
    }, [page, category, keyword]);

    // ── 조건을 바꾸면 항상 1페이지로 돌아간다.
    //    3페이지를 보다가 카테고리를 바꾸면 결과가 1페이지뿐일 수 있기 때문이다.
    const update = useCallback(
        (next) => {
            const merged = { category, keyword, page: 1, ...next };
            const out = {};
            if (merged.category) out.category = String(merged.category);
            if (merged.keyword) out.keyword = merged.keyword;
            if (merged.page > 1) out.page = String(merged.page);
            setParams(out);
        },
        [category, keyword, setParams],
    );

    const goPage = useCallback(
        (n) => {
            update({ page: n });
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },
        [update],
    );

    const filtering = category !== null || keyword !== '';

    return (
        <div className={styles.page}>
            <div className={styles.head}>
                <h1 className="title2">오늘의 메뉴</h1>
                <p className={`${styles.summary} label1-reading`}>
                    {menuPage ? `주문 가능한 메뉴 ${menuPage.totalElements}개` : error ? '' : '불러오는 중…'}
                </p>
            </div>

            <div className={styles.controls}>
                <CategoryTabs categories={categories} value={category} onChange={(c) => update({ category: c })} />
                <SearchField
                    value={keyword}
                    onChange={(k) => update({ keyword: k })}
                    label="메뉴 이름"
                    placeholder="메뉴 이름으로 검색"
                />
            </div>

            {error ? (
                <StatusBlock
                    tone="error"
                    title="메뉴를 불러오지 못했습니다"
                    description={error}
                    action={<Button onClick={() => window.location.reload()}>다시 시도</Button>}
                />
            ) : loading ? (
                <ul className={styles.grid}>
                    {Array.from({ length: PAGE_SIZE }, (_, i) => (
                        <li key={i}>
                            <MenuCardSkeleton />
                        </li>
                    ))}
                </ul>
            ) : menuPage.content.length === 0 ? (
                <StatusBlock
                    title="조건에 맞는 메뉴가 없습니다"
                    description="검색어를 바꾸거나 카테고리를 '전체'로 바꿔 보세요."
                    action={filtering && <Button onClick={() => setParams({})}>조건 초기화</Button>}
                />
            ) : (
                <>
                    <ul className={styles.grid}>
                        {menuPage.content.map((menu) => (
                            <li key={menu.menuCode}>
                                <MenuCard menu={menu} />
                            </li>
                        ))}
                    </ul>
                    <Pagination page={menuPage.number} lastPage={menuPage.totalPages} onChange={goPage} />
                </>
            )}
        </div>
    );
}
