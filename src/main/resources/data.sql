-- [임시 데이터] 개발 환경에서의 테스트 및 기능 확인을 위한 초기 관심사 데이터입니다.
-- TODO: 프로젝트 안정화 이후 Flyway 또는 Liquibase를 사용한 정식 마이그레이션 스크립트로 전환해야 합니다.

INSERT INTO interests (keyword) VALUES
('운동'),
('요리'),
('영화감상'),
('코딩'),
('음악감상'),
('여행'),
('독서'),
('게임'),
('반려동물'),
('재테크');

INSERT INTO ideal_categories (name) VALUES
('얼굴형'),
('성격'),
('체형');

-- 1. '얼굴형'에 대한 하위 옵션들 (category_id = 1)
INSERT INTO ideal_options (category_id, `name`) VALUES
(1, '강아지상'),
(1, '고양이상'),
(1, '토끼상'),
(1, '곰상'),
(1, '사슴상'),
(1, '계란형'),
(1, '둥근형'),
(1, '각진형');

-- 2. '성격'에 대한 하위 옵션들 (category_id = 2)
INSERT INTO ideal_options (category_id, `name`) VALUES
(2, '다정한'),
(2, '유머있는'),
(2, '진중한'),
(2, '리더십 있는'),
(2, '배려심 깊은'),
(2, '긍정적인'),
(2, '외향적인'),
(2, '내향적인'),
(2, '재치있는');

-- 3. '체형'에 대한 하위 옵션들 (category_id = 3)
INSERT INTO ideal_options (category_id, `name`) VALUES
(3, '마른'),
(3, '슬림탄탄'),
(3, '보통'),
(3, '통통한'),
(3, '글래머'),
(3, '근육질'),
(3, '어깨가 넓은');

