CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,          -- User.java의 username 필드
                       email VARCHAR(100) NOT NULL UNIQUE,             -- User.java의 email 필드
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN DEFAULT FALSE,                  -- User.java의 enabled 필드
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);