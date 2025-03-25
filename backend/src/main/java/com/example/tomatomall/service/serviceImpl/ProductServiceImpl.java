package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.po.Product;
import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    @Resource
    private ProductRepository productRepository;

    @Override
    public Response<List<Product>> getAllProducts() {
        return Response.buildSuccess(productRepository.findAll());
    }

    @Override
    public Response<Product> getProduct(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(Response::buildSuccess)
                .orElse(Response.buildFailure("商品不存在", "400"));
    }

    @Override
    @Transactional
    public Response<Product> createProduct(Product product) {
        return Response.buildSuccess(productRepository.save(product));
    }

    @Override
    @Transactional
    public Response<Product> updateProduct(Product product) {
        if (!productRepository.existsById(product.getId())) {
            return Response.buildFailure("商品不存在", "400");
        }
        return Response.buildSuccess(productRepository.save(product));
    }

    @Override
    @Transactional
    public Response<String> deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            return Response.buildFailure("商品不存在", "400");
        }
        productRepository.deleteById(id);
        return Response.buildSuccess("删除成功");
    }

    @Override
    @Transactional
    public Response<Integer> updateStock(Long productId, Integer amount) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            return Response.buildFailure("商品不存在", "400");
        }

        Product product = productOpt.get();
        int newStock = product.getStock() + amount;
        if (newStock < 0) {
            return Response.buildFailure("库存不足", "400");
        }

        product.setStock(newStock);
        productRepository.save(product);
        return Response.buildSuccess(newStock);
    }

    @Override
    public Response<Integer> getStock(Long productId) {
        Optional<Product> product = productRepository.findById(productId);
        return product.map(p -> Response.buildSuccess(p.getStock()))
                .orElse(Response.buildFailure("商品不存在", "400"));
    }
}