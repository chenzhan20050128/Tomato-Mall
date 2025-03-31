package com.example.tomatomall.service;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.vo.Response;
import java.util.List;

public interface ProductService {
    Response<List<Product>> getAllProducts();
    Response<Product> getProduct(Integer id);
    Response<Product> createProduct(Product product);
    Response<String> updateProduct(Product product);
    Response<String> deleteProduct(Integer id);
    Response<String> updateStock(Integer productId, Integer amount);
    Response<Stockpile> getStock(Integer productId);
}