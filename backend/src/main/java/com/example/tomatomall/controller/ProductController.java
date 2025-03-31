package com.example.tomatomall.controller;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Response<List<Product>> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Response<Product> getProduct(@PathVariable Integer id) {
        return productService.getProduct(id);
    }

    @PutMapping
    public Response<String> updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }

    @PostMapping
    public Response<Product> createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable Integer id) {
        return productService.deleteProduct(id);
    }

    @PatchMapping("/stockpile/{productId}")
    public Response<String> updateStock(
            @PathVariable Integer productId,
            @RequestParam(value = "amount", required = true) Integer amount) {
        return productService.updateStock(productId, amount);
    }

    @GetMapping("/stockpile/{productId}")
    public Response<Stockpile> getStock(@PathVariable Integer productId) {
        return productService.getStock(productId);
    }
}