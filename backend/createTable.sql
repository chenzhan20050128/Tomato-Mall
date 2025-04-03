-- 删除已存在的表（注意删除顺序，先删除有外键引用的表）
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stockpiles;
DROP TABLE IF EXISTS specifications;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS carts_orders_relation;

CREATE TABLE users (
                       userid INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
                       username VARCHAR(50) NOT NULL COMMENT '用户名，不允许为空',
                       password VARCHAR(100) NOT NULL COMMENT '用户密码，仅参与插入操作',
                       name VARCHAR(50) NOT NULL COMMENT '用户姓名，不允许为空',
                       avatar VARCHAR(255) COMMENT '用户头像链接',
                       telephone VARCHAR(11) UNIQUE COMMENT '用户手机号，格式需符合1开头的11位数字',
                       email VARCHAR(100) COMMENT '用户邮箱，格式需符合邮箱规范',
                       location VARCHAR(255) COMMENT '用户所在地',
                       role VARCHAR(50) NOT NULL DEFAULT 'user' COMMENT '用户角色 admin,user,merchant'
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
                                                                                                  'user' -- 角色：2-用户
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
                                                                                                  'user'
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
                                                                                                  'admin'
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
                                                                                                  'user'
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
                                                                                                  'user'
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
                                                                                                  'user'
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
                                                                                                  'merchant'
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
                                                                                                  'user'
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
                                                                                                  'user'
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
                                                                                                  'user'
                                                                                              );



-- 创建商品表
CREATE TABLE products (
                          id INT AUTO_INCREMENT PRIMARY KEY COMMENT '商品id',
                          title VARCHAR(50) NOT NULL COMMENT '商品名称，不允许为空',
                          price DECIMAL(10,2) NOT NULL COMMENT '商品价格，不允许为空，最低为0元',
                          rate DOUBLE NOT NULL DEFAULT 5.0 COMMENT '商品评分，最低0分，最高10分',
                          description VARCHAR(255) COMMENT '商品描述',
                          cover VARCHAR(500) COMMENT '商品封面url',
                          detail VARCHAR(500) COMMENT '商品详细说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 创建商品规格表
CREATE TABLE specifications (
                                id INT AUTO_INCREMENT PRIMARY KEY COMMENT '规格id',
                                item VARCHAR(50) NOT NULL COMMENT '规格名称，不允许为空',
                                value VARCHAR(255) NOT NULL COMMENT '规格内容，不允许为空',
                                product_id INT NOT NULL COMMENT '所属商品id，不允许为空',
                                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- 创建商品库存表
CREATE TABLE stockpiles (
                            id INT AUTO_INCREMENT PRIMARY KEY COMMENT '商品库存id',
                            product_id INT NOT NULL COMMENT '所属商品id，不允许为空',
                            amount INT NOT NULL DEFAULT 0 COMMENT '商品库存数，指可卖的商品数量，不允许为空',
                            frozen INT NOT NULL DEFAULT 0 COMMENT '商品库存冻结数，指不可卖的商品数量，不允许为空',
                            FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';

-- 插入商品数据
INSERT INTO products (title, price, rate, description, cover, detail) VALUES
                                                                          ('深入理解Java虚拟机', 99.50, 9.5, 'Java开发者必读经典', 'https://example.com/covers/jvm.jpg', '全面讲解JVM原理与实践'),
                                                                          ('代码整洁之道', 59.00, 9.2, '软件工程经典著作', 'https://example.com/covers/clean-code.jpg', '讲解代码整洁之道与最佳实践'),
                                                                          ('Spring实战', 89.00, 9.0, 'Spring框架权威指南', 'https://example.com/covers/spring.jpg', '深入浅出Spring核心原理');

-- 插入规格数据
INSERT INTO specifications (item, value, product_id) VALUES
-- Java虚拟机的规格
('作者', '周志明', 1),
('ISBN', '9787111641247', 1),
('页数', '540', 1),
('出版社', '机械工业出版社', 1),
('出版日期', '2019-12-01', 1),
-- 代码整洁之道的规格
('作者', 'Robert C. Martin', 2),
('ISBN', '9787115216878', 2),
('页数', '288', 2),
('出版社', '人民邮电出版社', 2),
('出版日期', '2010-01-01', 2),
-- Spring实战的规格
('作者', 'Craig Walls', 3),
('ISBN', '9787115417305', 3),
('页数', '624', 3),
('出版社', '人民邮电出版社', 3),
('出版日期', '2016-04-01', 3);

-- 插入库存数据
INSERT INTO stockpiles (product_id, amount, frozen) VALUES
                                                        (1, 100, 10),
                                                        (2, 80, 5),
                                                        (3, 120, 15);



CREATE TABLE carts (
                       cart_item_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Cart item ID',
                       user_id INT NOT NULL COMMENT 'User ID',
                       product_id INT NOT NULL COMMENT 'Product ID',
                       quantity INT NOT NULL DEFAULT 1 COMMENT 'Quantity',
                       FOREIGN KEY (user_id) REFERENCES users(userID) ON DELETE CASCADE,
                       FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) COMMENT='Cart items table';

CREATE TABLE orders (
                        order_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Order ID',
                        user_id INT NOT NULL COMMENT 'User ID',
                        total_amount DECIMAL(10,2) NOT NULL COMMENT 'Order total',
                        payment_method VARCHAR(50) NOT NULL COMMENT 'Payment method',
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status',
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
                        FOREIGN KEY (user_id) REFERENCES users(userid)
) COMMENT='Orders table';



-- Insert data into carts table
INSERT INTO carts (user_id, product_id, quantity) VALUES
                                                      (1, 1, 2),  -- User 1 adds 2 of product 1
                                                      (1, 2, 1),  -- User 1 adds 1 of product 2
                                                      (2, 3, 3),  -- User 2 adds 3 of product 3
                                                      (3, 1, 1),  -- User 3 adds 1 of product 1
                                                      (3, 2, 2);  -- User 3 adds 2 of product 2

-- Insert data into orders table
INSERT INTO orders (user_id, total_amount, payment_method, status) VALUES
                                                                       (1, 258.00, 'Credit Card', 'SHIPPED'),   -- User 1 places an order
                                                                       (2, 267.00, 'PayPal', 'DELIVERED'),        -- User 2 places an order
                                                                       (3, 177.00, 'Credit Card', 'PENDING'),  -- User 3 places an order
                                                                       (4, 99.50, 'PayPal', 'PROCESSING'),  -- User 4 places an order
                                                                       (5, 59.00, 'Credit Card', 'SHIPPED');   -- User 5 places an order



CREATE TABLE carts_orders_relation (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       cartitem_id INT,
                                       order_id INT
);