-- NOTE: 외래키 제약 조건은 생략하고 Index로 처리

-- =====================
-- Member 관련 테이블
-- =====================

CREATE TABLE member
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    handle     VARCHAR(30)  NOT NULL,
    is_deleted BOOLEAN               DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NULL,
    deleted_at TIMESTAMP    NULL,

    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_handle UNIQUE (handle)
);

CREATE INDEX idx_member_email ON member (email);
CREATE INDEX idx_member_handle ON member (handle);

CREATE TABLE member_profile
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT      NOT NULL,
    nickname    VARCHAR(50) NOT NULL,
    bio         VARCHAR(1000),
    avatar_path VARCHAR(255),
    updated_at  TIMESTAMP   NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_member_id UNIQUE (member_id)
);

-- =====================
-- Post 관련 테이블
-- =====================

CREATE TABLE post
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id      BIGINT    NOT NULL,
    content        VARCHAR(2000),
    parent_post_id BIGINT    NULL,
    quoted_post_id BIGINT    NULL,
    view_count     BIGINT    NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NULL,
    is_deleted     BOOLEAN            DEFAULT FALSE
);

CREATE INDEX idx_post_created_at ON post (created_at);
CREATE INDEX idx_post_author_id ON post (author_id);
CREATE INDEX idx_post_parent_post_id ON post (parent_post_id);
CREATE INDEX idx_post_quoted_post_id ON post (quoted_post_id);
CREATE INDEX idx_post_author_created ON post (author_id, created_at DESC);

CREATE TABLE media
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(1000) NOT NULL
);

CREATE TABLE post_media
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    media_id   BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_post_media_post_media UNIQUE (post_id, media_id)
);

CREATE INDEX idx_post_media_post_id ON post_media (post_id);
CREATE INDEX idx_post_media_media_id ON post_media (media_id);

-- =====================
-- 상호작용 관련 테이블
-- =====================

CREATE TABLE post_like
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    member_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_post_like_post_member UNIQUE (post_id, member_id)
);

CREATE INDEX idx_post_like_post_id ON post_like (post_id);
CREATE INDEX idx_post_like_member_id ON post_like (member_id);

CREATE TABLE repost
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    member_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_repost_post_member UNIQUE (post_id, member_id)
);

CREATE INDEX idx_repost_post_id ON repost (post_id);
CREATE INDEX idx_repost_member_id ON repost (member_id);

-- =====================
-- Follow 관련 테이블
-- =====================

CREATE TABLE follows
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    following_member_id BIGINT    NOT NULL,
    followed_member_id  BIGINT    NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_follows_following_followed UNIQUE (following_member_id, followed_member_id)
);

CREATE INDEX idx_follow_following_member_id ON follows (following_member_id);
CREATE INDEX idx_follow_followed_member_id ON follows (followed_member_id);
