
package com.example.tomatomall.po;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@Table(name = "order_items")
/*
在 JPA/Hibernate 中，这种设计是 ​完全正确且符合规范 的，不会出现你担心的问题。
JPA 关联映射的工作原理
​对象关联：OrderItem 实体类通过 @ManyToOne 关联 Order 和 Product 对象，这是 ​面向对象的设计。
​外键存储：数据库实际存储的是 order_id 和 product_id 字段（即外键），这是 ​关系型数据库的设计。
​自动转换：JPA/Hibernate 在 ​保存实体时 会自动提取关联对象的 ID（如 order.getOrderId()、product.getId()）并存入外键字段，无需手动处理。
​2. 你的代码验证
在你的 processPaymentCallback 方法中：

// 设置 Order 对象
orderItem.setOrder(order); // JPA 会自动提取 order.getOrderId() 存入 order_id 字段

// 设置 Product 对象
orderItem.setProduct(product); // JPA 会自动提取 product.getId() 存入 product_id 字段
​实际存储效果：数据库的 order_items 表中会正确写入 order_id 和 product_id。
​关联查询：通过 OrderItem 查询时，JPA 会根据外键自动加载关联的 Order 和 Product 对象。
 */
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;
    private BigDecimal price;
}