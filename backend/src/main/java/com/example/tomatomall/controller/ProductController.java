package com.example.tomatomall.controller;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Resource
    private ProductService productService;

    @GetMapping
    public Response<List<Product>> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Response<Product> getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping
    public Response<Product> createProduct(@Valid @RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping
    public Response<Product> updateProduct(@Valid @RequestBody Product product) {
        return productService.updateProduct(product);
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }

    @PatchMapping("/stockpile/{productId}")
    public Response<Integer> updateStock(
            @PathVariable Long productId,
            @RequestParam Integer amount) {
        return productService.updateStock(productId, amount);
    }

    @GetMapping("/stockpile/{productId}")
    public Response<Integer> getStock(@PathVariable Long productId) {
        return productService.getStock(productId);
    }
}