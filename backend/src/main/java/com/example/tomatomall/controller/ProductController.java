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
        try {
            List<Product> products = productService.getAllProducts();
            return Response.buildSuccess(products);
        } catch (Exception e) {
            return Response.buildFailure("获取商品列表失败", "500");
        }
    }

    @GetMapping("/{id}")
    public Response<Product> getProduct(@PathVariable Integer id) {
        try {
            Product product = productService.getProduct(id);
            return Response.buildSuccess(product);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("获取商品信息失败", "500");
        }
    }

    @PutMapping
    public Response<String> updateProduct(@RequestBody Product product) {
        try {
            productService.updateProduct(product);
            return Response.buildSuccess("更新成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("更新商品失败: " + e.getMessage(), "500");
        }
    }

    @PostMapping
    public Response<Product> createProduct(@RequestBody Product product) {
        try {
            Product createdProduct = productService.createProduct(product);
            return Response.buildSuccess(createdProduct);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("创建商品失败: " + e.getMessage(), "500");
        }
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable Integer id) {
        try {
            productService.deleteProduct(id);
            return Response.buildSuccess("删除成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("删除商品失败: " + e.getMessage(), "500");
        }
    }

    @PatchMapping("/stockpile/{productId}")
    public Response<String> updateStock(
            @PathVariable Integer productId,
            @RequestParam(value = "amount", required = true) Integer amount) {
        try {
            productService.updateStock(productId, amount);
            return Response.buildSuccess("库存更新成功");
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("更新库存失败: " + e.getMessage(), "500");
        }
    }

    @GetMapping("/stockpile/{productId}")
    public Response<Stockpile> getStock(@PathVariable Integer productId) {
        try {
            Stockpile stockpile = productService.getStock(productId);
            return Response.buildSuccess(stockpile);
        } catch (RuntimeException e) {
            return Response.buildFailure(e.getMessage(), "400");
        } catch (Exception e) {
            return Response.buildFailure("获取库存信息失败: " + e.getMessage(), "500");
        }
    }
}