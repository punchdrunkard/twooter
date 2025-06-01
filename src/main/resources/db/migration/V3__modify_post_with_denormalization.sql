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
ALTER TABLE follow DROP INDEX uk_follows_following_followed;
RENAME TABLE follows TO follow;

-- 컬럼명 변경

ALTER TABLE follow
    CHANGE COLUMN following_member_id follower_id BIGINT NOT NULL COMMENT '팔로우 하는 사용자',
    CHANGE COLUMN followed_member_id followee_id BIGINT NOT NULL COMMENT '팔로우 받는 사용자';

-- 인덱스명 변경
CREATE UNIQUE INDEX uk_follow_follower_followee ON follow (follower_id, followee_id);
CREATE INDEX idx_post_repost_of_id ON post (repost_of_id) COMMENT '리포스트 관계 조회';

-- 6. 인덱스 추가
CREATE INDEX idx_post_like_interaction ON post_like (post_id, member_id) COMMENT '사용자 좋아요 상태 빠른 조회';
CREATE INDEX idx_post_media_optimization ON post_media (post_id, media_id) COMMENT '포스트별 미디어 조회 최적화';
