DROP TABLE IF EXISTS users;
CREATE TABLE users (
    username VARCHAR(50) NOT NULL COMMENT '用户名，不允许为空',
    password VARCHAR(100) NOT NULL COMMENT '用户密码，仅参与插入操作',
    name VARCHAR(50) NOT NULL COMMENT '用户姓名，不允许为空',
    avatar VARCHAR(255) COMMENT '用户头像链接',
    telephone VARCHAR(11) UNIQUE COMMENT '用户手机号，格式需符合1开头的11位数字', 
    email VARCHAR(100) COMMENT '用户邮箱，格式需符合邮箱规范',
    location VARCHAR(255) COMMENT '用户所在地',
    role INT NOT NULL DEFAULT 2 COMMENT '用户角色：1-管理员，2-用户，3-商家',
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';


-- 插入第一条用户信息
/*
BCrypt的验证机制会使用哈希字符串中存储的完整信息（包括版本标识
2a、工作因子10、以及内置的盐值）来验证密码。当用户输入"123456"时：
BCrypt会从数据库中存储的哈希值 $2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K 提取出盐值
使用这个相同的盐值和工作因子，对用户输入的"123456"进行哈希计算
将计算结果与存储的哈希值进行比较
只要原始密码是"123456"，且存储的哈希值是使用BCrypt正确生成的，验证过程就会成功匹配，用户就能正常登录。
这正是BCrypt的设计优势之一：即使每次加密生成的哈希值都不同，但验证机制可以保证正确的密码总能通过验证。
*/
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'Zhang San', -- 用户名（唯一，不允许为空）
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K', -- 密码,加密后的密码，123456->$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K
    'Zhang San', -- 姓名（不允许为空）
    'https://example.com/avatars/johndoe1.jpg', -- 头像 URL
    '13312345678', -- 手机号（符合 1 开头的 11 位数字格式）
    'john.doe1@example.com', -- 邮箱（符合邮箱格式）
    'New York', -- 所在地
    2 -- 角色：2-用户
);

-- 插入第二条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'cz',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'cz',
    'https://example.com/avatars/janesmith2.jpg',
    '13291770128',
    'jane.smith2@example.com',
    'London',
    2
);

-- 插入第三条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'admin',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'admin',
    'https://example.com/avatars/peterjones3.jpg',
    '12312345678',
    'peter.jones3@example.com',
    'Paris',
    1
);

-- 插入第四条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'student',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'student',
    'https://example.com/avatars/marybrown4.jpg',
    '13300000001',
    'mary.brown4@example.com',
    'Tokyo',
    2
);

-- 插入第五条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'davidwilson5',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'David Wilson 5',
    'https://example.com/avatars/davidwilson5.jpg',
    '13544444445',
    'david.wilson5@example.com',
    'Sydney',
    2
);

-- 插入第六条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'sarahmiller6',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'Sarah Miller 6',
    'https://example.com/avatars/sarahmiller6.jpg',
    '13455555556',
    'sarah.miller6@example.com',
    'Berlin',
    2
);

-- 插入第七条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'kevinanderson7',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'Kevin Anderson 7',
    'https://example.com/avatars/kevinanderson7.jpg',
    '13366666667',
    'kevin.anderson7@example.com',
    'Rome',
    3
);

-- 插入第八条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'laurathomas8',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'Laura Thomas 8',
    'https://example.com/avatars/laurathomas8.jpg',
    '13277777778',
    'laura.thomas8@example.com',
    'Madrid',
    2
);

-- 插入第九条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'chrisjackson9',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'Chris Jackson 9',
    'https://example.com/avatars/chrisjackson9.jpg',
    '13188888889',
    'chris.jackson9@example.com',
    'Toronto',
    2
);

-- 插入第十条用户信息
INSERT INTO users (username, password, name, avatar, telephone, email, location, role) VALUES (
    'amandawhite10',
    '$2a$10$mAvRDmcltj4kJ.uWXkNmYekY7jmoRqBYMvi/DSC0Yy4Hib6fL3a8K',
    'Amanda White 10',
    'https://example.com/avatars/amandawhite10.jpg',
    '13099999990',
    'amanda.white10@example.com',
    'Melbourne',
    2
);