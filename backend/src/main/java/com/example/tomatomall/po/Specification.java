package com.example.tomatomall.po;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "specifications")
public class Specification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String item;//”类别“ ”作者“ “页数”
    private String value;//”文学“ ”余华 “

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // 删除 productId 字段
}