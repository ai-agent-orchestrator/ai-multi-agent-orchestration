-- ------------------------------------------------------------------
-- 오지라퍼 레스토랑 풀스택 프로젝트용 데이터베이스 생성
-- ------------------------------------------------------------------
-- 실습용 ohgiraffers 계정은 02_database 수업에서 이미 만들었다.
-- 여기서는 이 프로젝트 전용 데이터베이스만 새로 만들고 권한을 부여한다.
-- 기존 menudb를 그대로 쓰지 않는 이유는, 테이블 구조를 바꾸면
-- menudb를 사용하는 앞 챕터 프로젝트들이 영향을 받기 때문이다.

-- 1) root 계정으로 접속한 상태에서 실행한다.
USE mysql;

-- 2) 프로젝트용 데이터베이스 생성
CREATE DATABASE ohgirestaurant;

-- 데이터베이스 목록을 확인해 ohgirestaurant가 존재하는지 확인
SHOW DATABASES;

-- 3) 생성한 ohgirestaurant 데이터베이스에 대하여 ohgiraffers 유저에게 모든 권한을 부여
GRANT ALL PRIVILEGES ON ohgirestaurant.* TO 'ohgiraffers'@'%';

-- ohgiraffers 유저에게 부여된 권한 확인 (menudb, ohgirestaurant 등이 함께 조회되면 됨)
SHOW GRANTS FOR 'ohgiraffers'@'%';

-- 4) 이후 작업은 ohgiraffers 계정 접속기에서 진행한다.
-- 00_02_DB_SCRIPT.sql을 열고 아래처럼 데이터베이스를 선택한 뒤 전체 실행한다.
USE ohgirestaurant;
