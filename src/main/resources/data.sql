-- 학교 정보 추가 (이미 존재하면 무시)
INSERT IGNORE INTO school (school_key, school_name, score) VALUES ('DONGIN-HS-001', '동인고등학교', 0);

-- 관리자 유저 추가
-- password: 'password' (BCrypt encoded: $2a$10$8.UnVuG9HHgffUDAlk8qfOpNa.My7GZqZ.atej.tJcfv9z.a1gOsW)
INSERT IGNORE INTO users (username, email, password, role, school_id, enabled, score) 
SELECT '관리자', 'admin@naver.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOpNa.My7GZqZ.atej.tJcfv9z.a1gOsW', 'ADMIN', id, TRUE, 0
FROM school 
WHERE school_name = '동인고등학교';
