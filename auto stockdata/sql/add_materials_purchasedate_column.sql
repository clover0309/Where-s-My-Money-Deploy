-- usermaterials 테이블에 purchasedate 컬럼 추가
-- 사용자가 금을 매수한 날짜를 저장하는 컬럼

-- 1. purchasedate 컬럼 추가 (NULL 허용으로 먼저 추가)
ALTER TABLE usermaterials 
ADD COLUMN usermaterials_purchasedate DATE NULL;

-- 2. 기존 데이터의 purchasedate를 createdat 값으로 업데이트
UPDATE usermaterials 
SET usermaterials_purchasedate = DATE(usermaterials_createdat)
WHERE usermaterials_purchasedate IS NULL;

-- 3. NOT NULL 제약조건 추가
ALTER TABLE usermaterials 
MODIFY COLUMN usermaterials_purchasedate DATE NOT NULL;

-- 4. 확인
SELECT * FROM usermaterials LIMIT 10;
