package com.example.tomatomall.service;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.vo.Response;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProduct(Integer id);
    Product createProduct(Product product);
    String updateProduct(Product product);
    String deleteProduct(Integer id);
    String updateStock(Integer productId, Integer amount);
    Stockpile getStock(Integer productId);
}