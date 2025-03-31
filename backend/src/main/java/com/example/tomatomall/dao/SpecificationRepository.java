package com.example.tomatomall.dao;

import com.example.tomatomall.po.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, Integer> {

    // 根据商品ID删除规格
    @Modifying
    @Query("delete from Specification s where s.product.id = ?1")
    void deleteByProductId(Integer productId);
    // 根据商品ID查找规格
    List<Specification> findByProductId(Integer productId);

    // 批量保存规格
    @Override
    <S extends Specification> List<S> saveAll(Iterable<S> entities);
}