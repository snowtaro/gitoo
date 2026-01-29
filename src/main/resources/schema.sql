CREATE TABLE IF NOT EXISTS school (
    id INT AUTO_INCREMENT PRIMARY KEY,
    school_key VARCHAR(30) NOT NULL,
    school_name VARCHAR(100) NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_school_key (school_key),
    UNIQUE KEY uq_school_name (school_name)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    school_id INT NOT NULL,

    score BIGINT NOT NULL DEFAULT 0,

    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_school
    FOREIGN KEY (school_id) REFERENCES school(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
    ) ENGINE=InnoDB;

CREATE INDEX idx_users_school_id ON users(school_id);


CREATE TABLE IF NOT EXISTS game (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_code VARCHAR(50) NOT NULL UNIQUE,      -- '테트리스'와 같이 게임코드 설정
    game_name VARCHAR(100) NOT NULL,
    score_type ENUM('SCORE','TIME','COUNT') NOT NULL DEFAULT 'SCORE',
    max_score INT NULL,                         -- 없으면 NULL
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS season (
    id INT AUTO_INCREMENT PRIMARY KEY,
    season_name VARCHAR(100) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_season_time CHECK (end_at > start_at)
    ) ENGINE=InnoDB;

CREATE INDEX idx_season_active ON season(is_active, start_at, end_at);


CREATE TABLE IF NOT EXISTS game_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    game_id INT NOT NULL,
    season_id INT NULL,

    attempt_uuid CHAR(36) NOT NULL,
    score INT NOT NULL DEFAULT 0,

    time_ms INT NULL,

    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_attempt_uuid UNIQUE (attempt_uuid),

    CONSTRAINT fk_attempt_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

    CONSTRAINT fk_attempt_game
    FOREIGN KEY (game_id) REFERENCES game(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

    CONSTRAINT fk_attempt_season
    FOREIGN KEY (season_id) REFERENCES season(id)
    ON UPDATE CASCADE
    ON DELETE SET NULL
    ) ENGINE=InnoDB;

CREATE INDEX idx_attempt_user_time ON game_attempt(user_id, created_at);
CREATE INDEX idx_attempt_game_time ON game_attempt(game_id, created_at);
CREATE INDEX idx_attempt_season ON game_attempt(season_id);


CREATE TABLE IF NOT EXISTS school_score (
    season_id INT NOT NULL,
    school_id INT NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (season_id, school_id),

    CONSTRAINT fk_school_score_season
    FOREIGN KEY (season_id) REFERENCES season(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

    CONSTRAINT fk_school_score_school
    FOREIGN KEY (school_id) REFERENCES school(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE INDEX idx_school_score_score ON school_score(season_id, score);

CREATE TABLE IF NOT EXISTS room_members (
                                            id BIGINT NOT NULL AUTO_INCREMENT,
                                            room_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    role VARCHAR(10) NOT NULL,
    ready TINYINT(1) NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_room_member (room_id, user_id),
    KEY idx_room_members_room (room_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

