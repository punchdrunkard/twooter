-- 1. 기존 테이블 삭제
DROP TABLE IF EXISTS member_profile;
DROP TABLE IF EXISTS member;

-- 2. 통합된 새로운 member 테이블 생성
CREATE TABLE member
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    handle      VARCHAR(30)  NOT NULL,
    nickname    VARCHAR(50)  NOT NULL,
    bio         VARCHAR(1000),
    avatar_path VARCHAR(255),
    is_deleted  BOOLEAN               DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NULL,
    deleted_at  TIMESTAMP    NULL,

    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_handle UNIQUE (handle)
);

-- 3. 필요한 인덱스 생성
CREATE INDEX idx_member_email ON member (email);
CREATE INDEX idx_member_handle ON member (handle);
