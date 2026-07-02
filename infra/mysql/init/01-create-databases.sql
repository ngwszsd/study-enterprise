-- 首次初始化:创建两套独立 schema 与共用应用账号
CREATE DATABASE IF NOT EXISTS study_java
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS study_kotlin
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'study'@'%' IDENTIFIED BY 'study_pass';
GRANT ALL PRIVILEGES ON study_java.* TO 'study'@'%';
GRANT ALL PRIVILEGES ON study_kotlin.* TO 'study'@'%';
FLUSH PRIVILEGES;
