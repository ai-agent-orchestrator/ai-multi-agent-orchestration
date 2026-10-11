-- ------------------------------------------------------------------
-- 오지라퍼 레스토랑(ohgirestaurant) 테이블 생성 및 초기 데이터
-- ------------------------------------------------------------------
-- menudb의 카테고리·메뉴·주문·결제 구조를 뼈대로 하고, 회원 테이블을 더했다.
-- 모든 테이블은 기본키(PK)를 인조식별자 하나로만 둔다.
-- 두 테이블을 잇는 연관 테이블도 복합키 대신 자기 PK를 따로 두고,
-- 중복 방지는 UNIQUE 제약 조건으로 처리한다.
--
-- [Phase 2] Phase 1 스크립트와 다른 곳은 '3-1. 회원'의 비밀번호뿐이다. (평문 -> BCrypt 해시)
-- Phase 1 에서 쓰던 데이터베이스에 이 스크립트를 다시 실행하면 된다.

-- 1. 테이블 삭제 (자식 테이블부터 삭제한다)
DROP TABLE IF EXISTS tbl_cart CASCADE;
DROP TABLE IF EXISTS tbl_payment CASCADE;
DROP TABLE IF EXISTS tbl_order_menu CASCADE;
DROP TABLE IF EXISTS tbl_order CASCADE;
DROP TABLE IF EXISTS tbl_menu_allergen CASCADE;
DROP TABLE IF EXISTS tbl_allergen CASCADE;
DROP TABLE IF EXISTS tbl_menu CASCADE;
DROP TABLE IF EXISTS tbl_category CASCADE;
DROP TABLE IF EXISTS tbl_member CASCADE;

-- 2. 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_member
(
    -- column level constraints
    member_code INT AUTO_INCREMENT COMMENT '회원코드',
    member_id VARCHAR(30) NOT NULL COMMENT '아이디',
    member_password VARCHAR(255) NOT NULL COMMENT '비밀번호',
    member_name VARCHAR(30) NOT NULL COMMENT '회원이름',
    member_email VARCHAR(100) NOT NULL COMMENT '이메일',
    member_role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER' COMMENT '권한',
    -- table level constraints
    CONSTRAINT pk_member_code PRIMARY KEY (member_code),
    CONSTRAINT uk_member_id UNIQUE (member_id),
    CONSTRAINT ck_member_role CHECK (member_role IN ('ROLE_USER', 'ROLE_ADMIN'))
) ENGINE=INNODB COMMENT '회원';

-- 설명. 비밀번호 컬럼을 VARCHAR(255)로 넉넉하게 잡아둔 이유
--  Phase 1은 평문, Phase 2는 BCrypt 해시(60자)를 저장한다.
--  둘 다 담을 수 있게 해두어 Phase 를 바꿀 때 테이블 구조는 그대로 두고 데이터만 교체한다.

CREATE TABLE IF NOT EXISTS tbl_category
(
    -- column level constraints
    category_code INT AUTO_INCREMENT COMMENT '카테고리코드',
    category_name VARCHAR(30) NOT NULL COMMENT '카테고리명',
    ref_category_code INT COMMENT '상위카테고리코드',
    -- table level constraints
    CONSTRAINT pk_category_code PRIMARY KEY (category_code),
    CONSTRAINT fk_ref_category_code FOREIGN KEY (ref_category_code) REFERENCES tbl_category (category_code)
) ENGINE=INNODB COMMENT '카테고리';

CREATE TABLE IF NOT EXISTS tbl_menu
(
    -- column level constraints
    menu_code INT AUTO_INCREMENT COMMENT '메뉴코드',
    menu_name VARCHAR(30) NOT NULL COMMENT '메뉴명',
    menu_price INT NOT NULL COMMENT '메뉴가격',
    category_code INT NOT NULL COMMENT '카테고리코드',
    orderable_status CHAR(1) NOT NULL COMMENT '주문가능상태',
    menu_description VARCHAR(500) COMMENT '상세설명',
    menu_ingredients VARCHAR(300) COMMENT '재료',
    menu_image_name VARCHAR(100) COMMENT '이미지파일명',
    deleted_status CHAR(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부',
    -- table level constraints
    CONSTRAINT pk_menu_code PRIMARY KEY (menu_code),
    CONSTRAINT fk_category_code FOREIGN KEY (category_code) REFERENCES tbl_category (category_code),
    CONSTRAINT ck_orderable_status CHECK (orderable_status IN ('Y', 'N')),
    CONSTRAINT ck_deleted_status CHECK (deleted_status IN ('Y', 'N'))
) ENGINE=INNODB COMMENT '메뉴';

-- 설명. orderable_status와 deleted_status는 용도가 다르다.
--  - orderable_status : 매진, 재료 소진, 시즌 종료 등으로 '지금' 주문할 수 없는 상태 (다시 'Y'로 돌릴 수 있다)
--  - deleted_status   : 관리자가 삭제한 메뉴 (손님과 관리자 목록 모두에서 사라진다)
--  메뉴를 DELETE로 실제 삭제하지 않는 이유는 주문 이력(tbl_order_menu)이 이 메뉴를 참조하고 있기 때문이다.
--  실제로 지우면 외래키 제약 조건에 걸리거나, 지난 주문 내역에서 어떤 메뉴를 주문했는지 알 수 없게 된다.
--  그래서 삭제 여부만 표시해두는 방식(soft delete)을 사용한다.

-- 설명. menu_image_name에는 파일이 저장된 경로가 아니라 '저장된 파일명'만 넣는다.
--  파일이 저장되는 폴더는 application.yaml 설정으로 관리한다.
--  이렇게 해두면 나중에 저장 위치를 바꿔도(예: 클라우드 저장소) DB 데이터는 그대로 둘 수 있다.

CREATE TABLE IF NOT EXISTS tbl_allergen
(
    -- column level constraints
    allergen_code INT AUTO_INCREMENT COMMENT '알레르기코드',
    allergen_name VARCHAR(30) NOT NULL COMMENT '알레르기명',
    -- table level constraints
    CONSTRAINT pk_allergen_code PRIMARY KEY (allergen_code),
    CONSTRAINT uk_allergen_name UNIQUE (allergen_name)
) ENGINE=INNODB COMMENT '알레르기';

CREATE TABLE IF NOT EXISTS tbl_menu_allergen
(
    -- column level constraints
    menu_allergen_code INT AUTO_INCREMENT COMMENT '메뉴별알레르기코드',
    menu_code INT NOT NULL COMMENT '메뉴코드',
    allergen_code INT NOT NULL COMMENT '알레르기코드',
    -- table level constraints
    CONSTRAINT pk_menu_allergen_code PRIMARY KEY (menu_allergen_code),
    CONSTRAINT uk_menu_allergen UNIQUE (menu_code, allergen_code),
    CONSTRAINT fk_menu_allergen_menu_code FOREIGN KEY (menu_code) REFERENCES tbl_menu (menu_code),
    CONSTRAINT fk_menu_allergen_allergen_code FOREIGN KEY (allergen_code) REFERENCES tbl_allergen (allergen_code)
) ENGINE=INNODB COMMENT '메뉴별알레르기';

-- 설명. 메뉴와 알레르기는 다대다(N:M) 관계다.
--  (메뉴 하나에 알레르기가 여러 개, 알레르기 하나가 여러 메뉴에 들어간다)
--  이를 연관 테이블로 풀되, 복합키 (menu_code, allergen_code) 대신 인조식별자 하나를 PK로 둔다.
--  같은 메뉴에 같은 알레르기가 두 번 들어가는 것은 UNIQUE 제약 조건이 막아준다.

CREATE TABLE IF NOT EXISTS tbl_order
(
    -- column level constraints
    order_code INT AUTO_INCREMENT COMMENT '주문코드',
    member_code INT NOT NULL COMMENT '주문회원코드',
    order_datetime DATETIME NOT NULL COMMENT '주문일시',
    total_order_price INT NOT NULL COMMENT '총주문금액',
    -- table level constraints
    CONSTRAINT pk_order_code PRIMARY KEY (order_code),
    CONSTRAINT fk_order_member_code FOREIGN KEY (member_code) REFERENCES tbl_member (member_code)
) ENGINE=INNODB COMMENT '주문';

CREATE TABLE IF NOT EXISTS tbl_order_menu
(
    -- column level constraints
    order_menu_code INT AUTO_INCREMENT COMMENT '주문별메뉴코드',
    order_code INT NOT NULL COMMENT '주문코드',
    menu_code INT NOT NULL COMMENT '메뉴코드',
    order_amount INT NOT NULL COMMENT '주문수량',
    -- table level constraints
    CONSTRAINT pk_order_menu_code PRIMARY KEY (order_menu_code),
    CONSTRAINT uk_order_menu UNIQUE (order_code, menu_code),
    CONSTRAINT fk_order_menu_order_code FOREIGN KEY (order_code) REFERENCES tbl_order (order_code),
    CONSTRAINT fk_order_menu_menu_code FOREIGN KEY (menu_code) REFERENCES tbl_menu (menu_code)
) ENGINE=INNODB COMMENT '주문별메뉴';

CREATE TABLE IF NOT EXISTS tbl_payment
(
    -- column level constraints
    payment_code INT AUTO_INCREMENT COMMENT '결제코드',
    order_code INT NOT NULL COMMENT '주문코드',
    payment_datetime DATETIME NOT NULL COMMENT '결제일시',
    payment_price INT NOT NULL COMMENT '결제금액',
    payment_type VARCHAR(10) NOT NULL COMMENT '결제수단',
    -- table level constraints
    CONSTRAINT pk_payment_code PRIMARY KEY (payment_code),
    CONSTRAINT uk_payment_order_code UNIQUE (order_code),
    CONSTRAINT fk_payment_order_code FOREIGN KEY (order_code) REFERENCES tbl_order (order_code),
    CONSTRAINT ck_payment_type CHECK (payment_type IN ('CARD', 'CASH'))
) ENGINE=INNODB COMMENT '결제';

-- 설명. menudb에서는 주문과 결제 사이에 tbl_payment_order라는 연관 테이블이 있었다.
--  이 프로젝트에서는 주문 1건에 결제 1건만 일어나므로(1:1), 결제 테이블이 주문코드를 직접 가진다.
--  order_code에 UNIQUE 제약 조건을 걸어 한 주문에 결제가 두 번 생기는 것을 막는다.

CREATE TABLE IF NOT EXISTS tbl_cart
(
    -- column level constraints
    cart_code INT AUTO_INCREMENT COMMENT '장바구니코드',
    member_code INT NOT NULL COMMENT '회원코드',
    menu_code INT NOT NULL COMMENT '메뉴코드',
    cart_amount INT NOT NULL COMMENT '담은수량',
    -- table level constraints
    CONSTRAINT pk_cart_code PRIMARY KEY (cart_code),
    CONSTRAINT uk_cart UNIQUE (member_code, menu_code),
    CONSTRAINT fk_cart_member_code FOREIGN KEY (member_code) REFERENCES tbl_member (member_code),
    CONSTRAINT fk_cart_menu_code FOREIGN KEY (menu_code) REFERENCES tbl_menu (menu_code),
    CONSTRAINT ck_cart_amount CHECK (cart_amount BETWEEN 1 AND 99)
) ENGINE=INNODB COMMENT '장바구니';

-- 설명. 장바구니는 회원과 메뉴의 다대다(N:M) 관계를 풀어낸 연관 테이블이다. 한 행이 '장바구니에 담긴 메뉴 한 줄'이다.
--  같은 회원이 같은 메뉴를 두 줄로 담지 않도록 (member_code, menu_code)에 UNIQUE 제약 조건을 건다.
--  다시 담으면 새 행을 만들지 않고 수량(cart_amount)을 더한다.

-- 3. 초기 데이터
-- 3-1. 회원
-- 설명. [Phase 2] 비밀번호는 BCrypt 해시로 저장한다. 원래 비밀번호는 Phase 1 과 같다.
--  admin / admin1234,  user01 / user1234,  user02 / user1234
--  user01 과 user02 는 비밀번호가 같은데 해시값은 다르다. BCrypt 가 매번 다른 salt 를 섞기 때문이다.
--  그래서 해시값끼리 비교해서는 안 되고, 로그인할 때 PasswordEncoder.matches() 로 비교한다.
INSERT INTO tbl_member (member_id, member_password, member_name, member_email, member_role)
VALUES ('admin', '$2a$10$miGUHS5vGHkbadGh/K5Dred0avJujBrV7uTLxdTDlnHyJsJ2O61lW', '관리자', 'admin@ohgiraffers.com', 'ROLE_ADMIN');
INSERT INTO tbl_member (member_id, member_password, member_name, member_email, member_role)
VALUES ('user01', '$2a$10$IF1QOp1ma7ROfEExY7G8fuqUgYoVt0VfYquseDCEReYAAKvJ1weRq', '오지라퍼', 'user01@ohgiraffers.com', 'ROLE_USER');
INSERT INTO tbl_member (member_id, member_password, member_name, member_email, member_role)
VALUES ('user02', '$2a$10$51hzahQ9nRZKmuuBwd6MseZoBexYqCfk6mOkMjOlABhp.3SiLBPva', '김기린', 'user02@ohgiraffers.com', 'ROLE_USER');

-- 3-2. 카테고리 (menudb와 동일)
INSERT INTO tbl_category VALUES (null, '식사', null);
INSERT INTO tbl_category VALUES (null, '음료', null);
INSERT INTO tbl_category VALUES (null, '디저트', null);
INSERT INTO tbl_category VALUES (null, '한식', 1);
INSERT INTO tbl_category VALUES (null, '중식', 1);
INSERT INTO tbl_category VALUES (null, '일식', 1);
INSERT INTO tbl_category VALUES (null, '퓨전', 1);
INSERT INTO tbl_category VALUES (null, '커피', 2);
INSERT INTO tbl_category VALUES (null, '쥬스', 2);
INSERT INTO tbl_category VALUES (null, '기타', 2);
INSERT INTO tbl_category VALUES (null, '동양', 3);
INSERT INTO tbl_category VALUES (null, '서양', 3);

-- 3-3. 알레르기 (식품의약품안전처 알레르기 유발물질 표시 대상 기준)
INSERT INTO tbl_allergen (allergen_name) VALUES ('알류');
INSERT INTO tbl_allergen (allergen_name) VALUES ('우유');
INSERT INTO tbl_allergen (allergen_name) VALUES ('메밀');
INSERT INTO tbl_allergen (allergen_name) VALUES ('땅콩');
INSERT INTO tbl_allergen (allergen_name) VALUES ('대두');
INSERT INTO tbl_allergen (allergen_name) VALUES ('밀');
INSERT INTO tbl_allergen (allergen_name) VALUES ('고등어');
INSERT INTO tbl_allergen (allergen_name) VALUES ('게');
INSERT INTO tbl_allergen (allergen_name) VALUES ('새우');
INSERT INTO tbl_allergen (allergen_name) VALUES ('돼지고기');
INSERT INTO tbl_allergen (allergen_name) VALUES ('복숭아');
INSERT INTO tbl_allergen (allergen_name) VALUES ('토마토');
INSERT INTO tbl_allergen (allergen_name) VALUES ('아황산류');
INSERT INTO tbl_allergen (allergen_name) VALUES ('호두');
INSERT INTO tbl_allergen (allergen_name) VALUES ('닭고기');
INSERT INTO tbl_allergen (allergen_name) VALUES ('쇠고기');
INSERT INTO tbl_allergen (allergen_name) VALUES ('오징어');
INSERT INTO tbl_allergen (allergen_name) VALUES ('조개류');
INSERT INTO tbl_allergen (allergen_name) VALUES ('잣');

-- 3-4. 메뉴 (menudb의 메뉴 36개에 상세설명·재료·이미지를 더했다)
-- 설명. 1~21번 메뉴는 이미지 파일이 준비되어 있다. (seed-images 폴더 참고)
--  22~36번 메뉴는 이미지가 없으며(NULL), 화면에서는 기본 이미지를 보여준다.
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('열무김치라떼', 4500, 8, 'Y',
        '에스프레소와 우유에 잘 익은 열무김치 국물을 더한 라떼다. 새콤한 맛과 고소한 맛이 번갈아 느껴진다.',
        '에스프레소, 우유, 열무김치 국물, 설탕', '06a0060ae2da4dffb9a8a440ba5d9c5e.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('우럭스무디', 5000, 10, 'Y',
        '구운 우럭살을 플레인 요거트, 얼음과 함께 갈아 만든 스무디다. 차갑고 짭조름하다.',
        '우럭, 플레인 요거트, 얼음, 꿀', 'fcb3e0c8f94940cf99724d26e6020259.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('생갈치쉐이크', 6000, 10, 'Y',
        '생갈치와 바닐라 아이스크림을 함께 갈아 만든 쉐이크다. 달콤하고 시원하지만 바다 향이 강하다.',
        '갈치, 바닐라 아이스크림, 우유, 얼음', '8e2492fd197e42d5855ffbbb5142b4ed.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('갈릭미역파르페', 7000, 10, 'Y',
        '달콤한 생크림 사이에 마늘 칩과 미역 줄기를 층층이 쌓은 차가운 파르페다.',
        '생크림, 마늘 칩, 미역 줄기, 그래놀라, 꿀', '58b3fd68f6074de2b33d4430fd29244b.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('앙버터김치찜', 13000, 4, 'N',
        '돼지고기 김치찜 위에 팥앙금과 버터를 올렸다. 매콤하고 달콤하며 따뜻하게 먹는 메뉴다.',
        '묵은지, 돼지고기 목살, 팥앙금, 버터, 두부', '7580adcf59d04240b7a16f6cf07bd34b.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('생마늘샐러드', 12000, 4, 'Y',
        '통 생마늘을 듬뿍 올린 채소 샐러드에 간장 드레싱을 곁들였다. 알싸하고 상큼하다.',
        '생마늘, 양상추, 방울토마토, 간장 드레싱, 참깨', '7b91aee3ddec49a69a9b7d2849493f7f.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('민트미역국', 15000, 4, 'Y',
        '쇠고기 미역국에 민트 잎을 띄워 향을 더했다. 따뜻하고 뒷맛이 개운하다.',
        '미역, 쇠고기 양지, 국간장, 민트 잎, 참기름', 'af732dfe3e4f482bac8d1ef0bd9be02b.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('한우딸기국밥', 20000, 4, 'Y',
        '한우 사골 국물로 끓인 국밥 위에 생딸기를 얹었다. 뜨거운 국물과 새콤달콤한 딸기를 함께 먹는다.',
        '한우 사골, 한우 양지, 밥, 생딸기, 대파', '8a4cd876df574970a565b41e47561080.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('홍어마카롱', 9000, 12, 'Y',
        '삭힌 홍어를 넣은 크림을 채운 마카롱이다. 겉은 달콤하고 속은 톡 쏘는 맛이 난다.',
        '아몬드 가루, 달걀흰자, 설탕, 홍어, 버터크림', 'c0a177a658b44f749699f91a23c47d8b.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('코다리마늘빵', 7000, 12, 'N',
        '마늘버터를 바른 바게트 사이에 매콤한 코다리조림을 넣었다. 따뜻할 때 먹어야 맛있다.',
        '바게트, 마늘버터, 코다리, 고추장', '053626c2d16f4814a5e81b842a115dc7.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('정어리빙수', 10000, 10, 'Y',
        '우유 얼음 위에 정어리 조림과 팥, 연유를 올린 빙수다. 아주 시원하고 달콤짭짤하다.',
        '우유 얼음, 정어리, 팥, 연유, 떡', '323a5df17163482d90a74f8198a4e4c6.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('날치알스크류바', 2000, 10, 'Y',
        '딸기 맛 얼음과자에 날치알을 박아 넣어 씹을 때마다 톡톡 터진다. 가장 저렴한 메뉴다.',
        '딸기 시럽, 얼음, 날치알', 'd97144a76a7b42fabfcf8a5662762040.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('직화구이젤라또', 8000, 12, 'Y',
        '바닐라 젤라또의 겉면을 토치로 그을려 불향을 입혔다. 차갑지만 스모키한 향이 난다.',
        '우유, 생크림, 바닐라, 설탕, 달걀노른자', 'a4195b272f764a3ab81e85fbbba6c067.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('과메기커틀릿', 13000, 6, 'Y',
        '과메기에 빵가루를 입혀 돈가스처럼 튀겼다. 바삭하고 고소하며 소스가 새콤하다.',
        '과메기(꽁치), 빵가루, 밀가루, 달걀, 돈가스 소스', '702215dbe9784ebf92561d5504b0b5a6.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('죽방멸치튀김우동', 11000, 6, 'N',
        '죽방멸치를 통째로 튀겨 따뜻한 우동 위에 올렸다. 국물이 진하고 짭짤하다.',
        '우동면, 죽방멸치, 튀김옷, 가쓰오 육수, 쪽파', '5c226e59e7c342608d220ce0c476d01f.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('흑마늘아메리카노', 9000, 8, 'Y',
        '아메리카노에 흑마늘 청을 넣었다. 쌉싸름한 커피 맛 뒤에 은은한 단맛이 남는다.',
        '에스프레소, 흑마늘 청, 물', '684fd2cccfc74ad4944619bc72f76788.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('아이스가리비관자육수', 6000, 10, 'Y',
        '가리비 관자로 우려낸 육수를 차갑게 식혀 얼음과 함께 낸다. 시원하고 감칠맛이 깊다.',
        '가리비 관자, 다시마, 무, 얼음', 'b150ff66223a46adabb75a71299cb25a.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('붕어빵초밥', 35000, 6, 'Y',
        '붕어빵 틀에 초밥을 넣고 구웠다. 팥 대신 연어와 새우가 들어 있는 가장 비싼 메뉴다.',
        '초밥, 연어, 새우, 붕어빵 반죽, 팥', 'c260a42b01394faba92ed9a7ca868aa9.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('까나리코코넛쥬스', 9000, 9, 'Y',
        '코코넛 워터에 까나리액젓을 한 스푼 넣었다. 달고 짜고 시원하다.',
        '코코넛 워터, 까나리액젓, 라임', '21406eb90b2f4ae09ea0af015d2df6fe.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('마라깐쇼한라봉', 22000, 5, 'N',
        '한라봉과 새우를 마라 깐쇼 소스에 볶았다. 혀가 얼얼하게 맵고 새콤하다.',
        '한라봉, 새우, 마라 소스, 케첩, 전분', 'eccf4c58d72241ddb4c84401485d4363.png');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('돌미나리백설기', 5000, 11, 'Y',
        '쌀가루에 돌미나리를 섞어 쪄낸 백설기다. 향긋하고 담백하다.',
        '쌀가루, 돌미나리, 설탕', '9a6c1b13af0a469fa2d9e3084f6e438c.PNG');
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('우주정복 마카롱', 5000, 12, 'Y',
        '보랏빛 꼬끄에 블루베리 크림을 채운 마카롱이다. 새콤달콤하다.',
        '아몬드 가루, 달걀흰자, 설탕, 블루베리, 버터크림', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('초콜릿 피자', 8000, 7, 'Y',
        '피자 도우 위에 다크 초콜릿, 마시멜로, 바나나, 호두를 올려 구웠다. 따뜻하고 아주 달다.',
        '피자 도우, 다크 초콜릿, 마시멜로, 바나나, 호두', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('모험가의 미트볼', 15000, 4, 'Y',
        '쇠고기와 돼지고기를 섞은 미트볼을 매콤한 토마토 소스에 졸였다. 든든한 식사 메뉴다.',
        '쇠고기, 돼지고기, 빵가루, 달걀, 토마토 소스', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('바다의 속삭임 스무디', 6500, 10, 'Y',
        '파란 스피룰리나와 파인애플, 코코넛 밀크를 갈았다. 시원하고 상큼하다.',
        '스피룰리나, 파인애플, 코코넛 밀크, 얼음', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('달빛 요정 케이크', 7500, 12, 'Y',
        '레몬 크림과 복숭아 콤포트를 층층이 올린 시폰 케이크다. 부드럽고 상큼하다.',
        '밀가루, 달걀, 레몬 크림, 복숭아, 생크림', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('해적왕의 보물 상자', 12000, 11, 'Y',
        '약과, 깨강정, 땅콩엿을 나무 상자에 담은 한과 모둠이다. 달고 고소하다.',
        '약과, 깨강정, 땅콩엿, 잣, 조청', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('신비로운 유니콘 아이스크림', 5500, 10, 'Y',
        '딸기, 바닐라, 민트 세 가지 색 아이스크림 위에 무지개 스프링클을 뿌렸다. 달콤하고 시원하다.',
        '우유, 생크림, 딸기, 민트, 스프링클', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('드래곤 숨결 핫소스', 3000, 1, 'Y',
        '청양고추와 하바네로로 만든 아주 매운 소스다. 다른 메뉴에 곁들여 먹는다.',
        '청양고추, 하바네로, 식초, 마늘', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('불타는 치킨', 9900, 1, 'N',
        '드래곤 숨결 핫소스로 버무린 매운 닭강정에 땅콩 분태를 뿌렸다.',
        '닭고기, 튀김가루, 핫소스, 물엿, 땅콩 분태', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('펭귄의 꿈 파르페', 8500, 10, 'Y',
        '그릭 요거트와 블루베리 위에 화이트 초콜릿 펭귄을 올린 파르페다. 차갑고 새콤달콤하다.',
        '그릭 요거트, 블루베리, 그래놀라, 화이트 초콜릿', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('외계인의 초록 젤리', 4200, 11, 'Y',
        '말차와 청포도로 만든 탱글탱글한 젤리다. 가볍고 시원하다.',
        '말차, 청포도, 한천, 설탕', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('마법사의 램버거', 11000, 1, 'Y',
        '양고기 패티에 체다 치즈와 토마토를 올린 버거다. 육즙이 많고 든든하다.',
        '양고기 패티, 브리오슈 번, 체다 치즈, 토마토, 양파', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('귀신이 곡한 누룽지', 7000, 5, 'Y',
        '바삭한 누룽지 위에 뜨거운 해산물 소스를 부어 소리가 나는 누룽지탕이다.',
        '누룽지, 오징어, 새우, 청경채, 굴소스', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('백년초 스무디', 5600, 10, 'Y',
        '백년초 열매로 만든 분홍빛 스무디다. 새콤하고 시원하다.',
        '백년초, 요거트, 꿀, 얼음', NULL);
INSERT INTO tbl_menu (menu_name, menu_price, category_code, orderable_status, menu_description, menu_ingredients, menu_image_name)
VALUES ('초능력자의 에너지볼', 9500, 2, 'Y',
        '땅콩버터와 대추야자를 뭉친 에너지볼에 두유 한 잔을 곁들인 세트다.',
        '땅콩버터, 대추야자, 귀리, 두유', NULL);

-- 3-5. 메뉴별 알레르기
-- 설명. (메뉴코드, 알레르기코드) 쌍이다. 알레르기가 없는 메뉴(12, 16, 19, 21, 25, 29, 32번)는 행이 없다.
--  알레르기코드: 1 알류, 2 우유, 3 메밀, 4 땅콩, 5 대두, 6 밀, 7 고등어, 8 게, 9 새우, 10 돼지고기,
--             11 복숭아, 12 토마토, 13 아황산류, 14 호두, 15 닭고기, 16 쇠고기, 17 오징어, 18 조개류, 19 잣
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (1, 2), (1, 9);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (2, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (3, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (4, 2), (4, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (5, 10), (5, 2), (5, 5), (5, 9);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (6, 12), (6, 5), (6, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (7, 16), (7, 5), (7, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (8, 16);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (9, 1), (9, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (10, 6), (10, 2), (10, 5);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (11, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (13, 2), (13, 1);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (14, 6), (14, 1), (14, 5), (14, 12);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (15, 6), (15, 5);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (17, 18);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (18, 9), (18, 6), (18, 1), (18, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (20, 9), (20, 12), (20, 5), (20, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (22, 1), (22, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (23, 6), (23, 2), (23, 14);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (24, 16), (24, 10), (24, 6), (24, 1), (24, 12);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (26, 6), (26, 1), (26, 2), (26, 11);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (27, 6), (27, 4), (27, 19);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (28, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (30, 15), (30, 6), (30, 4);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (31, 2), (31, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (33, 6), (33, 2), (33, 1), (33, 12);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (34, 17), (34, 9), (34, 18), (34, 5), (34, 6);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (35, 2);
INSERT INTO tbl_menu_allergen (menu_code, allergen_code) VALUES (36, 4), (36, 5);

-- 3-6. 주문 · 주문별 메뉴 · 결제
-- 설명. user01(회원코드 2)이 지난 주문 내역을 가지고 있도록 미리 넣어둔다.
--  총주문금액과 결제금액은 (메뉴 가격 × 수량)의 합과 일치해야 한다.
-- 주문 1: 열무김치라떼 4,500 × 2 + 달빛 요정 케이크 7,500 × 1 = 16,500
INSERT INTO tbl_order (member_code, order_datetime, total_order_price) VALUES (2, '2026-09-18 12:30:00', 16500);
INSERT INTO tbl_order_menu (order_code, menu_code, order_amount) VALUES (1, 1, 2), (1, 26, 1);
INSERT INTO tbl_payment (order_code, payment_datetime, payment_price, payment_type) VALUES (1, '2026-09-18 12:30:00', 16500, 'CARD');

-- 주문 2: 모험가의 미트볼 15,000 × 1 + 백년초 스무디 5,600 × 1 = 20,600
INSERT INTO tbl_order (member_code, order_datetime, total_order_price) VALUES (2, '2026-09-25 18:10:00', 20600);
INSERT INTO tbl_order_menu (order_code, menu_code, order_amount) VALUES (2, 24, 1), (2, 35, 1);
INSERT INTO tbl_payment (order_code, payment_datetime, payment_price, payment_type) VALUES (2, '2026-09-25 18:10:00', 20600, 'CASH');

-- 주문 3: user02(회원코드 3) 초콜릿 피자 8,000 × 2 = 16,000
INSERT INTO tbl_order (member_code, order_datetime, total_order_price) VALUES (3, '2026-09-27 15:00:00', 16000);
INSERT INTO tbl_order_menu (order_code, menu_code, order_amount) VALUES (3, 23, 2);
INSERT INTO tbl_payment (order_code, payment_datetime, payment_price, payment_type) VALUES (3, '2026-09-27 15:00:00', 16000, 'CARD');

-- 3-7. 장바구니
-- 설명. user02(회원코드 3)가 메뉴 두 개를 담아 둔 상태로 시작한다. (다른 회원의 장바구니를 보는 문제점 확인용)
INSERT INTO tbl_cart (member_code, menu_code, cart_amount) VALUES (3, 2, 1), (3, 11, 2);

COMMIT;
