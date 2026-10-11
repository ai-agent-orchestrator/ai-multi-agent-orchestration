import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router';

import { createMenu, updateMenu } from '../../api/adminApi.js';
import { fetchAllergens, fetchCategories, fetchMenu } from '../../api/menuApi.js';
import Button from '../../components/Button.jsx';
import Field from '../../components/Field.jsx';
import MenuDraftAssistant from '../../components/MenuDraftAssistant.jsx';
import MenuImage from '../../components/MenuImage.jsx';
import StatusBlock from '../../components/StatusBlock.jsx';
import fieldStyles from '../../components/Field.module.css';
import styles from './AdminMenuFormPage.module.css';

const EMPTY_FORM = {
    menuName: '',
    menuPrice: '',
    categoryCode: '',
    orderableStatus: 'Y',
    menuDescription: '',
    menuIngredients: '',
    allergenCodes: [],
};

/**
 * 메뉴 등록과 수정이 같은 화면을 쓴다.
 *   /admin/menus/new            -> 등록 (menuCode 없음)
 *   /admin/menus/:menuCode/edit -> 수정 (기존 값을 받아 채운다)
 *
 * 이 화면은 제어 컴포넌트(value + onChange)로 만들었다.
 * 수정 화면은 서버에서 받은 기존 값으로 입력칸을 채워야 하고, 알레르기 체크박스처럼
 * 여러 값을 다루는 입력이 있어서 상태로 들고 있는 편이 다루기 쉽다.
 */
export default function AdminMenuFormPage() {
    const { menuCode } = useParams();
    const isEdit = menuCode !== undefined;
    const navigate = useNavigate();

    const [categories, setCategories] = useState([]);
    const [allergens, setAllergens] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [currentImageUrl, setCurrentImageUrl] = useState(null);
    const [imageFile, setImageFile] = useState(null);
    // [Phase 3-2 선택] AI 가 사진을 만드는 동안 보여줄 미리보기 (Data URL)
    const [aiImagePreview, setAiImagePreview] = useState(null);
    const [loadError, setLoadError] = useState(null);
    const [submitError, setSubmitError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    // ── 선택 목록(카테고리·알레르기)과, 수정이면 기존 메뉴 값을 받는다.
    useEffect(() => {
        let alive = true;

        async function load() {
            try {
                const [categoryList, allergenList, menu] = await Promise.all([
                    fetchCategories(),
                    fetchAllergens(),
                    isEdit ? fetchMenu(menuCode) : Promise.resolve(null),
                ]);
                if (!alive) return;

                setCategories(categoryList);
                setAllergens(allergenList);
                if (menu) {
                    setForm({
                        menuName: menu.menuName,
                        menuPrice: String(menu.menuPrice),
                        categoryCode: String(menu.categoryCode),
                        orderableStatus: menu.orderableStatus,
                        menuDescription: menu.menuDescription ?? '',
                        menuIngredients: menu.menuIngredients ?? '',
                        allergenCodes: menu.allergens.map((allergen) => allergen.allergenCode),
                    });
                    setCurrentImageUrl(menu.menuImageUrl);
                }
            } catch (err) {
                if (alive) setLoadError(err.message);
            }
        }

        load();
        return () => {
            alive = false;
        };
    }, [isEdit, menuCode]);

    // ── 고른 이미지 파일을 업로드하기 전에 미리 보여준다.
    //    createObjectURL 로 만든 주소는 다 쓰면 revokeObjectURL 로 돌려줘야 메모리가 풀린다.
    const previewUrl = useMemo(() => (imageFile ? URL.createObjectURL(imageFile) : null), [imageFile]);
    useEffect(() => {
        return () => {
            if (previewUrl) URL.revokeObjectURL(previewUrl);
        };
    }, [previewUrl]);

    // 상위 카테고리 아래에 하위 카테고리를 묶어서 보여준다. (<optgroup>)
    const categoryGroups = useMemo(() => {
        const parents = categories.filter((category) => category.refCategoryCode === null);
        return parents.map((parent) => ({
            parent,
            children: categories.filter((category) => category.refCategoryCode === parent.categoryCode),
        }));
    }, [categories]);

    function change(name, value) {
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    // [Phase 3-2] AI 초안 도우미에게 넘길 메뉴 정보. 카테고리는 코드 대신 이름을 보낸다. (AI 가 읽을 글이므로)
    function menuForDraft() {
        const category = categories.find((item) => String(item.categoryCode) === form.categoryCode);
        return {
            menuName: form.menuName,
            categoryName: category?.categoryName ?? '',
            menuPrice: form.menuPrice === '' ? null : Number(form.menuPrice),
            menuIngredients: form.menuIngredients,
            menuDescription: form.menuDescription,
        };
    }

    function toggleAllergen(allergenCode) {
        setForm((prev) => ({
            ...prev,
            allergenCodes: prev.allergenCodes.includes(allergenCode)
                ? prev.allergenCodes.filter((code) => code !== allergenCode)
                : [...prev.allergenCodes, allergenCode],
        }));
    }

    async function handleSubmit(e) {
        e.preventDefault();

        /*
         * 서버의 MenuRequest 는 @ModelAttribute 로 multipart/form-data 를 받는다.
         * FormData 에 넣는 이름(첫 번째 인자)이 MenuRequest 의 필드 이름과 같아야 값이 채워진다.
         *
         *   formData.append('menuName', ...)       -> MenuRequest.menuName
         *   formData.append('allergenCodes', 2)    -> MenuRequest.allergenCodes (List)
         *   formData.append('allergenCodes', 9)       같은 이름으로 여러 번 넣으면 목록이 된다
         *   formData.append('menuImage', file)     -> 컨트롤러의 MultipartFile menuImage 파라미터
         */
        const formData = new FormData();
        formData.append('menuName', form.menuName);
        formData.append('menuPrice', form.menuPrice);
        formData.append('categoryCode', form.categoryCode);
        formData.append('orderableStatus', form.orderableStatus);
        formData.append('menuDescription', form.menuDescription);
        formData.append('menuIngredients', form.menuIngredients);
        form.allergenCodes.forEach((code) => formData.append('allergenCodes', code));
        // 새 이미지를 고른 경우에만 보낸다. 수정할 때 보내지 않으면 서버는 기존 이미지를 유지한다.
        if (imageFile) {
            formData.append('menuImage', imageFile);
        }

        setSubmitting(true);
        setSubmitError(null);
        try {
            if (isEdit) {
                await updateMenu(menuCode, formData);
            } else {
                await createMenu(formData);
            }
            navigate('/admin/menus');
        } catch (err) {
            // 400 이면 서버의 검증 메시지(description - detail)가 그대로 보인다.
            setSubmitError(err.message);
            setSubmitting(false);
        }
    }

    if (loadError) {
        return <StatusBlock tone="error" title="메뉴 정보를 불러오지 못했습니다" description={loadError} />;
    }

    return (
        <form className={styles.page} onSubmit={handleSubmit}>
            <h1 className="title2">{isEdit ? '메뉴 수정' : '메뉴 등록'}</h1>

            <div className={styles.layout}>
                <section className={styles.panel}>
                    <Field label="메뉴명" htmlFor="menuName">
                        <input
                            id="menuName"
                            className={`${fieldStyles.control} body1`}
                            value={form.menuName}
                            onChange={(e) => change('menuName', e.target.value)}
                            maxLength={30}
                            required
                        />
                    </Field>

                    <div className={styles.row}>
                        <Field label="가격(원)" htmlFor="menuPrice">
                            <input
                                id="menuPrice"
                                type="number"
                                min={0}
                                step={100}
                                className={`${fieldStyles.control} body1`}
                                value={form.menuPrice}
                                onChange={(e) => change('menuPrice', e.target.value)}
                                required
                            />
                        </Field>

                        <Field label="카테고리" htmlFor="categoryCode">
                            <select
                                id="categoryCode"
                                className={`${fieldStyles.control} body1`}
                                value={form.categoryCode}
                                onChange={(e) => change('categoryCode', e.target.value)}
                                required
                            >
                                <option value="">선택하세요</option>
                                {categoryGroups.map(({ parent, children }) => (
                                    <optgroup key={parent.categoryCode} label={parent.categoryName}>
                                        <option value={parent.categoryCode}>{parent.categoryName} (분류 없음)</option>
                                        {children.map((child) => (
                                            <option key={child.categoryCode} value={child.categoryCode}>
                                                {child.categoryName}
                                            </option>
                                        ))}
                                    </optgroup>
                                ))}
                            </select>
                        </Field>
                    </div>

                    <fieldset className={styles.fieldset}>
                        <legend className="label1">주문 가능 상태</legend>
                        <div className={styles.options}>
                            {[
                                ['Y', '주문 가능'],
                                ['N', '주문 불가 (품절·재료 소진·시즌 종료)'],
                            ].map(([value, label]) => (
                                <label key={value} className={`${styles.option} body2`}>
                                    <input
                                        type="radio"
                                        name="orderableStatus"
                                        value={value}
                                        checked={form.orderableStatus === value}
                                        onChange={(e) => change('orderableStatus', e.target.value)}
                                    />
                                    {label}
                                </label>
                            ))}
                        </div>
                    </fieldset>

                    <Field label="상세설명" htmlFor="menuDescription" hint="맛과 특징을 적는다. 500자 이하">
                        <textarea
                            id="menuDescription"
                            rows={4}
                            className={`${fieldStyles.control} body1`}
                            value={form.menuDescription}
                            onChange={(e) => change('menuDescription', e.target.value)}
                            maxLength={500}
                        />
                    </Field>
                    {/* [Phase 3-2] 메뉴명 · 카테고리 · 가격 · 재료를 보고 AI 가 설명 칸을 채운다. (WebSocket) */}
                    <MenuDraftAssistant
                        getMenu={menuForDraft}
                        onChange={(text) => change('menuDescription', text)}
                        onImageProgress={setAiImagePreview}
                        onImage={setImageFile}
                    />

                    <Field label="재료" htmlFor="menuIngredients" hint="쉼표로 구분한다. 300자 이하">
                        <input
                            id="menuIngredients"
                            className={`${fieldStyles.control} body1`}
                            value={form.menuIngredients}
                            onChange={(e) => change('menuIngredients', e.target.value)}
                            maxLength={300}
                        />
                    </Field>

                    <fieldset className={styles.fieldset}>
                        <legend className="label1">알레르기 유발 성분</legend>
                        <div className={styles.allergens}>
                            {allergens.map((allergen) => (
                                <label key={allergen.allergenCode} className={`${styles.option} body2`}>
                                    <input
                                        type="checkbox"
                                        checked={form.allergenCodes.includes(allergen.allergenCode)}
                                        onChange={() => toggleAllergen(allergen.allergenCode)}
                                    />
                                    {allergen.allergenName}
                                </label>
                            ))}
                        </div>
                    </fieldset>
                </section>

                <section className={styles.panel}>
                    <Field label="메뉴 이미지" htmlFor="menuImage" hint={isEdit ? '새 이미지를 고르지 않으면 기존 이미지를 유지한다.' : '선택 사항이다.'}>
                        <input
                            id="menuImage"
                            type="file"
                            accept="image/*"
                            className="body2"
                            onChange={(e) => setImageFile(e.target.files[0] ?? null)}
                        />
                    </Field>
                    <div className={styles.preview}>
                        {aiImagePreview ? (
                            <img src={aiImagePreview} alt="AI 가 만드는 중인 이미지" />
                        ) : previewUrl ? (
                            <img src={previewUrl} alt="선택한 이미지 미리보기" />
                        ) : (
                            <MenuImage path={currentImageUrl} alt="현재 이미지" />
                        )}
                    </div>
                </section>
            </div>

            {submitError && <p className={`${fieldStyles.error} body2`}>{submitError}</p>}

            <div className={styles.actions}>
                <Button variant="secondary" onClick={() => navigate('/admin/menus')}>
                    취소
                </Button>
                <Button type="submit" disabled={submitting}>
                    {submitting ? '저장하는 중…' : isEdit ? '수정 완료' : '등록하기'}
                </Button>
            </div>
        </form>
    );
}
