-- 1. post 테이블 구조 변경

-- repost_of_id 컬럼 추가 (리포스트 기능용)
ALTER TABLE post
    ADD COLUMN repost_of_id BIGINT NULL COMMENT '리포스트인 경우 원본 포스트 ID' AFTER quoted_post_id;

-- 반정규화 필드 추가
ALTER TABLE post
    ADD COLUMN like_count   BIGINT NOT NULL DEFAULT 0 COMMENT '좋아요 수 (반정규화)' AFTER view_count,
    ADD COLUMN repost_count BIGINT NOT NULL DEFAULT 0 COMMENT '리포스트 수 (반정규화)' AFTER like_count;

-- 2. 불필요한 테이블 제거
-- repost 테이블 제거 (post.repost_of_id로 대체됨)
DROP TABLE IF EXISTS repost;

-- 3. follow 테이블 네이밍 변경

RENAME TABLE follows TO follow;

-- 컬럼명도 더 명확하게 변경
ALTER TABLE follow
    CHANGE COLUMN following_member_id follower_id BIGINT NOT NULL COMMENT '팔로우 하는 사용자',
    CHANGE COLUMN followed_member_id followee_id BIGINT NOT NULL COMMENT '팔로우 받는 사용자';

-- 인덱스명 변경
DROP INDEX uk_follows_following_followed;
CREATE UNIQUE INDEX uk_follow_follower_followee ON follow (follower_id, followee_id);

CREATE INDEX idx_post_repost_of_id ON post (repost_of_id) COMMENT '리포스트 관계 조회';

-- 6. 테이블 코멘트 추가
-- 테이블별 코멘트
ALTER TABLE member
    COMMENT = '사용자 정보 테이블';
ALTER TABLE post
    COMMENT = '포스트 테이블 (일반포스트, 답글, 인용, 리포스트 모두 포함)';
ALTER TABLE post_like
    COMMENT = '포스트 좋아요 관계 테이블';
ALTER TABLE follows
    COMMENT = '사용자 팔로우 관계 테이블';
ALTER TABLE media
    COMMENT = '미디어 파일 정보 테이블';
ALTER TABLE post_media
    COMMENT = '포스트-미디어 연결 테이블';

-- 주요 컬럼 코멘트
ALTER TABLE post
    MODIFY COLUMN parent_post_id BIGINT NULL COMMENT '답글인 경우 부모 포스트 ID';
ALTER TABLE post
    MODIFY COLUMN quoted_post_id BIGINT NULL COMMENT '인용인 경우 원본 포스트 ID';
ALTER TABLE post
    MODIFY COLUMN view_count BIGINT NOT NULL DEFAULT 0 COMMENT '조회수 (반정규화)';

ALTER TABLE follows
    MODIFY COLUMN following_member_id BIGINT NOT NULL COMMENT '팔로우 하는 사용자 ID';
ALTER TABLE follows
    MODIFY COLUMN followed_member_id BIGINT NOT NULL COMMENT '팔로우 받는 사용자 ID';


-- 7. 인덱스 추가
CREATE INDEX idx_post_like_interaction ON post_like (post_id, member_id) COMMENT '사용자 좋아요 상태 빠른 조회';
CREATE INDEX idx_post_media_optimization ON post_media (post_id, media_id) COMMENT '포스트별 미디어 조회 최적화';
