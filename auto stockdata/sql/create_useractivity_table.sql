-- useractivity 테이블 생성 SQL
CREATE TABLE IF NOT EXISTS useractivity (
    userActivity_idx INT AUTO_INCREMENT PRIMARY KEY,
    userActivity_userId VARCHAR(50) NOT NULL,
    userActivity_type VARCHAR(50) NOT NULL,
    userActivity_timestamp DATETIME NOT NULL,
    INDEX idx_userId (userActivity_userId),
    INDEX idx_type (userActivity_type),
    INDEX idx_timestamp (userActivity_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 테이블 확인
SELECT * FROM useractivity ORDER BY userActivity_timestamp DESC LIMIT 10;
