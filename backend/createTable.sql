USE tomatomall;

-- 删除已存在的表（注意删除顺序，先删除有外键引用的表）
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stockpiles;
DROP TABLE IF EXISTS specifications;
DROP TABLE IF EXISTS comment_replies;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS carts_orders_relation;
DROP TABLE IF EXISTS advertisements;
DROP TABLE IF EXISTS products;

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
                                product_id INT NOT NULL COMMENT '所属商品id，不允许为空'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';



-- 创建商品库存表
CREATE TABLE stockpiles (
                            id INT AUTO_INCREMENT PRIMARY KEY COMMENT '商品库存id',
                            product_id INT NOT NULL COMMENT '所属商品id，不允许为空',
                            amount INT NOT NULL DEFAULT 0 COMMENT '商品库存数，指可卖的商品数量，不允许为空',
                            frozen INT NOT NULL DEFAULT 0 COMMENT '商品库存冻结数，指不可卖的商品数量，不允许为空',
                            locked_amount INT NOT NULL DEFAULT 0 COMMENT '锁定库存数，指已被锁定但未支付的商品数量，不允许为空',
                            lock_expire_time DATETIME NULL COMMENT '锁定过期时间, 超过此时间自动释放锁定库存'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';
-- 插入商品数据
INSERT INTO products (title, price, rate, description, cover, detail) VALUES
                                                                          ('深入理解Java虚拟机', 99.50, 9.5, 'Java开发者必读经典', 'https://example.com/covers/jvm.jpg', '全面讲解JVM原理与实践'),
                                                                          ('代码整洁之道', 59.00, 9.2, '软件工程经典著作', 'https://example.com/covers/clean-code.jpg', '讲解代码整洁之道与最佳实践'),
                                                                          ('Spring实战', 89.00, 9.0, 'Spring框架权威指南', 'https://example.com/covers/spring.jpg', '深入浅出Spring核心原理'),
                                                                          ('百年孤独', 39.00, 9.7, '魔幻现实主义文学巨著', 'https://example.com/covers/one-hundred-years-of-solitude.jpg', '讲述布恩迪亚家族七代人的传奇故事'),
                                                                          ('小王子', 25.00, 9.8, '献给每一个曾经是孩子的大人', 'https://example.com/covers/the-little-prince.jpg', '一部充满哲理和诗意的童话'),
                                                                          ('追风筝的人', 45.00, 9.6, '关于爱、背叛与救赎的故事', 'https://example.com/covers/the-kite-runner.jpg', '展现阿富汗的社会变迁与人性的复杂'),
                                                                          ('人类简史：从动物到上帝', 65.00, 9.4, '重新审视人类历史与未来', 'https://example.com/covers/sapiens.jpg', '探讨人类进化、文化发展与未来走向'),
                                                                          ('时间简史', 55.00, 9.3, '了解宇宙的入门指南', 'https://example.com/covers/a-brief-history-of-time.jpg', '讲解宇宙的起源、发展与基本原理'),
                                                                          ('三体', 50.00, 9.9, '中国科幻文学的里程碑', 'https://example.com/covers/the-three-body-problem.jpg', '讲述地球文明与三体文明的对抗'),
                                                                          ('活着', 30.00, 9.5, '余华经典之作', 'https://example.com/covers/to-live.jpg', '讲述主人公福贵坎坷的一生');

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
                                                         ('出版日期', '2016-04-01', 3),
                                                         -- 百年孤独的规格
                                                         ('作者', '加西亚·马尔克斯', 4),
                                                         ('ISBN', '9787532704496', 4),
                                                         ('译者', '高长荣', 4),
                                                         ('出版社', '上海译文出版社', 4),
                                                         ('出版日期', '2011-06-01', 4),
                                                         -- 小王子的规格
                                                         ('作者', '安托万·德·圣埃克苏佩里', 5),
                                                         ('ISBN', '9787532758833', 5),
                                                         ('译者', '李继宏', 5),
                                                         ('出版社', '上海译文出版社', 5),
                                                         ('出版日期', '2015-01-01', 5),
                                                         -- 追风筝的人的规格
                                                         ('作者', '卡勒德·胡赛尼', 6),
                                                         ('ISBN', '9787208072402', 6),
                                                         ('译者', '李继宏', 6),
                                                         ('出版社', '上海人民出版社', 6),
                                                         ('出版日期', '2007-01-01', 6),
                                                         -- 人类简史：从动物到上帝的规格
                                                         ('作者', '尤瓦尔·赫拉利', 7),
                                                         ('ISBN', '9787536692132', 7),
                                                         ('译者', '林俊宏', 7),
                                                         ('出版社', '中信出版社', 7),
                                                         ('出版日期', '2014-11-01', 7),
                                                         -- 时间简史的规格
                                                         ('作者', '斯蒂芬·霍金', 8),
                                                         ('ISBN', '9787535482177', 8),
                                                         ('译者', '许明贤', 8),
                                                         ('出版社', '湖南科学技术出版社', 8),
                                                         ('出版日期', '2015-05-01', 8),
                                                         -- 三体的规格
                                                         ('作者', '刘慈欣', 9),
                                                         ('ISBN', '9787536692934', 9),
                                                         ('丛书', '三体', 9),
                                                         ('出版社', '重庆出版社', 9),
                                                         ('出版日期', '2008-01-01', 9),
                                                         -- 活着的规格
                                                         ('作者', '余华', 10),
                                                         ('ISBN', '9787544291681', 10),
                                                         ('装帧', '平装', 10),
                                                         ('出版社', '南海出版公司', 10),
                                                         ('出版日期', '2015-02-01', 10);

-- 插入库存数据
INSERT INTO stockpiles (product_id, amount, frozen, locked_amount, lock_expire_time) VALUES
                                                                                         (1, 100, 10, 0, NULL),
                                                                                         (2, 80, 5, 0, NULL),
                                                                                         (3, 120, 15, 0, NULL);

CREATE TABLE carts (
                       cart_item_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Cart item ID',
                       user_id INT NOT NULL COMMENT 'User ID',
                       product_id INT NOT NULL COMMENT 'Product ID',
                       quantity INT NOT NULL DEFAULT 1 COMMENT 'Quantity'
) COMMENT='Cart items table';

CREATE TABLE orders (
                        order_id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Order ID',
                        user_id INT NOT NULL COMMENT 'User ID',
                        username VARCHAR(50) NOT NULL COMMENT 'Username',
                        total_amount DECIMAL(10,2) NOT NULL COMMENT 'Order total',
                        payment_method VARCHAR(50) NOT NULL COMMENT 'Payment method',
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Order status (PENDING, SUCCESS, FAILED, TIMEOUT)',
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
                        trade_no VARCHAR(255) COMMENT 'Alipay trade number',
                        payment_time TIMESTAMP COMMENT 'Payment time',
                        lock_expire_time DATETIME COMMENT 'Inventory lock time'
) COMMENT='Orders table';



-- Insert data into carts table
INSERT INTO carts (user_id, product_id, quantity) VALUES
                                                      (1, 1, 2),  -- User 1 adds 2 of product 1
                                                      (1, 2, 1),  -- User 1 adds 1 of product 2
                                                      (2, 3, 3),  -- User 2 adds 3 of product 3
                                                      (3, 1, 1),  -- User 3 adds 1 of product 1
                                                      (3, 2, 2);  -- User 3 adds 2 of product 2


CREATE TABLE carts_orders_relation (
                                       id INT AUTO_INCREMENT PRIMARY KEY,
                                       cartitem_id INT,
                                       order_id INT
);



CREATE TABLE advertisements (
                                id INT AUTO_INCREMENT PRIMARY KEY COMMENT '广告id',
                                title VARCHAR(50) NOT NULL COMMENT '广告标题，不允许为空',
                                content VARCHAR(500) NOT NULL COMMENT '广告内容',
                                image_url VARCHAR(500) NOT NULL COMMENT '广告图片url',
                                product_id INT NOT NULL COMMENT '所属商品id，不允许为空'
) COMMENT='广告表';

-- 评论主表
CREATE TABLE comments (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          product_id INT NOT NULL,
                          user_id INT NOT NULL,
                          content TEXT NOT NULL,
                          rating DECIMAL(2,1) NOT NULL,
                          images VARCHAR(1000),
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '0-待审核 1-已发布 2-已删除',
                          CONSTRAINT fk_comments_product FOREIGN KEY (product_id) REFERENCES products(id),
                          CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(userid)
);

-- 评论回复表
CREATE TABLE comment_replies (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 comment_id INT NOT NULL,
                                 user_id INT NOT NULL,
                                 reply_to_user_id INT,
                                 content TEXT NOT NULL,
                                 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 status TINYINT NOT NULL DEFAULT 1 COMMENT '0-待审核 1-已发布 2-已删除',
                                 CONSTRAINT fk_replies_comment FOREIGN KEY (comment_id) REFERENCES comments(id),
                                 CONSTRAINT fk_replies_user FOREIGN KEY (user_id) REFERENCES users(userid),
                                 CONSTRAINT fk_replies_reply_user FOREIGN KEY (reply_to_user_id) REFERENCES users(userid)
);

-- 插入评论数据
INSERT INTO comments (product_id, user_id, content, rating, images, create_time, status) VALUES
-- 深入理解Java虚拟机的评论
(1, 1, '这本书非常详细地解释了JVM的工作原理，是Java开发者必备的参考书。', 5.0, 'https://example.com/comment_imgs/img1.jpg,https://example.com/comment_imgs/img2.jpg', '2023-10-15 10:30:00', 1),
(1, 2, '内容很丰富，但有些章节理解起来比较困难，需要反复阅读。', 4.0, 'https://example.com/comment_imgs/img3.jpg', '2023-10-20 15:45:00', 1),
(1, 3, '买来学习JVM调优的，确实讲解得很透彻。', 4.5, NULL, '2023-11-05 09:20:00', 1),

-- 代码整洁之道的评论
(2, 1, '改变了我写代码的方式，强烈推荐每个程序员阅读！', 5.0, NULL, '2023-09-10 14:25:00', 1),
(2, 4, '很实用的编程建议，但有些原则在实际项目中难以严格执行。', 4.0, 'https://example.com/comment_imgs/img4.jpg', '2023-09-25 11:30:00', 1),

-- Spring实战的评论
(3, 5, '通俗易懂，案例丰富，非常适合Spring入门和进阶。', 5.0, NULL, '2023-10-05 16:40:00', 1),
(3, 2, '内容有点过时了，希望能更新一版涵盖最新的Spring功能。', 3.5, NULL, '2023-10-12 13:15:00', 1),
(3, 6, '作为Spring框架的参考书很不错，但是部分代码示例有误。', 4.0, 'https://example.com/comment_imgs/img5.jpg,https://example.com/comment_imgs/img6.jpg', '2023-11-01 17:50:00', 1),

-- 百年孤独的评论
(4, 3, '文学巨著，晦涩但值得细细品味。', 4.5, NULL, '2023-08-20 10:10:00', 1),
(4, 7, '情节复杂，人物众多，初读有些混乱，需要耐心。', 4.0, NULL, '2023-09-15 12:30:00', 1),

-- 小王子的评论
(5, 8, '小时候看是童话，长大后看是人生，永远的经典。', 5.0, 'https://example.com/comment_imgs/img7.jpg', '2023-10-25 18:20:00', 1);

-- 插入评论回复数据
INSERT INTO comment_replies (comment_id, user_id, reply_to_user_id, content, create_time, status) VALUES
-- 回复第一条评论
(1, 2, 1, '同意你的观点，这本书对深入理解JVM非常有帮助！', '2023-10-15 11:45:00', 1),
(1, 3, 1, '你对书中的GC部分有什么看法？我觉得那部分写得特别好。', '2023-10-16 09:30:00', 1),
(1, 5, 3, '我也认为GC部分讲得很透彻，尤其是ZGC的解析。', '2023-10-16 10:15:00', 1),

-- 回复第四条评论
(4, 3, 1, '我也被这本书深深影响，改变了很多不良的编码习惯。', '2023-09-11 16:20:00', 1),
(4, 6, 3, '你能分享一下具体哪些编码习惯被改变了吗？', '2023-09-12 08:45:00', 1),
(4, 3, 6, '主要是命名规范和函数长度控制，以及更注重代码可读性。', '2023-09-12 09:30:00', 1),

-- 回复第六条评论
(6, 7, 5, '我也觉得这本书讲解Spring很清晰，特别适合入门。', '2023-10-06 10:25:00', 1),
(6, 4, 7, '你是如何结合实际项目学习的？有什么建议？', '2023-10-07 14:50:00', 1),

-- 回复第十条评论
(10, 6, 8, '小王子确实是本值得反复阅读的书，每个年龄段都有不同感悟。', '2023-10-26 09:15:00', 1),
(10, 9, 6, '最喜欢书中"只有用心才能看清楚"那句话，很有哲理。', '2023-10-27 11:30:00', 1);




-- id主键索引已存在，需检查product_id是否建索引
CREATE INDEX idx_product_id ON advertisements (product_id);


SELECT COUNT(*) FROM comments WHERE product_id = 1 AND status = 1;