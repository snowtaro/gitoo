CREATE TABLE schools (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         school_key VARCHAR(50) NOT NULL,
                         school_name VARCHAR(255) NOT NULL,
                         school_type VARCHAR(50) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE KEY uq_school_key (school_key)
);

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       school_id BIGINT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                       KEY idx_users_school_id (school_id),
                       CONSTRAINT fk_users_school
                           FOREIGN KEY (school_id) REFERENCES schools(id)
);
