
-- 관리자 권한/로그인 기록 컬럼 추가
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER nickname,
    ADD COLUMN last_login_at DATETIME NULL AFTER token_version;

-- 검색/필터 인덱스
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role ON users(role);