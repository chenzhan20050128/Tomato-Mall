package com.example.tomatomall.service;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.vo.Response;

import java.util.List;

public interface ProductService {
    Response<List<Product>> getAllProducts();
    Response<Product> getProduct(Long id);
    Response<Product> createProduct(Product product);
    Response<Product> updateProduct(Product product);
    Response<String> deleteProduct(Long id);
    Response<Integer> updateStock(Long productId, Integer amount);
    Response<Integer> getStock(Long productId);
}