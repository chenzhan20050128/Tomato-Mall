package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dao.SpecificationRepository;
import com.example.tomatomall.dao.StockpileRepository;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Specification;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Override
    @Transactional(readOnly = true)
    public Response<List<Product>> getAllProducts() {
        try {
            List<Product> products = productRepository.findAllWithSpecifications();
            return Response.buildSuccess(products);
        } catch (Exception e) {
            return Response.buildFailure("获取商品列表失败", "500");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response<Product> getProduct(Integer id) {
        try {
            Optional<Product> productOptional = Optional.ofNullable(productRepository.findByIdWithSpecifications(id));
            if (productOptional.isPresent()) {
                Product product = productOptional.get();
                // 确保小数点后两位
                if (product.getPrice() != null) {
                    product.setPrice(product.getPrice().setScale(2));
                }
                return Response.buildSuccess(product);
            }
            return Response.buildFailure("商品不存在", "400");
        } catch (Exception e) {
            return Response.buildFailure("获取商品信息失败", "500");
        }
    }

    @Override
    @Transactional
    public Response<Product> createProduct(Product product) {
        try {
            // 基本验证
            if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
                return Response.buildFailure("商品标题不能为空", "400");
            }
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Response.buildFailure("商品价格必须大于0", "400");
            }

            // 设置新商品
            product.setId(null);
            if (product.getRate() == null) {
                product.setRate(0.0);
            }

            // 处理规格信息
            List<Specification> specs = product.getSpecifications();
            product.setSpecifications(null);

            // 先保存商品
            Product savedProduct = productRepository.save(product);

            // 处理规格
            if (specs != null) {
                specs.forEach(spec -> {
                    spec.setId(null);
                    spec.setProduct(savedProduct);
                });
                savedProduct.setSpecifications(specs);
                productRepository.save(savedProduct);
            }

            // 创建库存记录
            Stockpile stockpile = new Stockpile();
            stockpile.setProductId(savedProduct.getId());
            stockpile.setAmount(0);
            stockpile.setFrozen(0);
            stockpileRepository.save(stockpile);

            return Response.buildSuccess(savedProduct);
        } catch (Exception e) {
            throw new RuntimeException("创建商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response<String> updateProduct(Product product) {
        if (product.getId() == null) {
            return Response.buildFailure("商品ID不能为空", "400");
        }

        try {
            // 检查商品是否存在
            Product existingProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            // 更新基本信息
            existingProduct.setTitle(product.getTitle());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setRate(product.getRate());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setCover(product.getCover());
            existingProduct.setDetail(product.getDetail());

            // 删除旧的规格信息
            specificationRepository.deleteByProductId(product.getId());

            // 添加新的规格信息
            if (product.getSpecifications() != null) {
                for (Specification spec : product.getSpecifications()) {
                    spec.setId(null); // 确保是新建规格
                    spec.setProduct(existingProduct); // 只设置 product 关联
                }
                existingProduct.setSpecifications(product.getSpecifications());
            }

            // 保存更新
            productRepository.save(existingProduct);
            return Response.buildSuccess("更新成功");
        } catch (Exception e) {
            return Response.buildFailure("更新商品失败: " + e.getMessage(), "500");
        }
    }

    @Override
    @Transactional
    public Response<String> deleteProduct(Integer id) {
        try {
            // 检查商品是否存在
            Product product = productRepository.findByIdWithSpecifications(id);
            if (product == null) {
                return Response.buildFailure("商品不存在", "400");
            }

            // 清空规格关联
            product.getSpecifications().clear();

            // 删除商品（由于配置了 cascade = CascadeType.ALL，规格会自动删除）
            productRepository.delete(product);

            return Response.buildSuccess("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.buildFailure("删除商品失败: " + e.getMessage(), "500");
        }
    }

    @Override
    @Transactional
    public Response<String> updateStock(Integer productId, Integer amount) {
        try {
            // 验证参数
            if (amount == null || amount < 0) {
                return Response.buildFailure("库存数量不能为负数", "400");
            }

            // 检查商品是否存在
            Product product = productRepository.findById(productId)
                    .orElse(null);
            if (product == null) {
                return Response.buildFailure("商品不存在", "400");
            }

            // 获取并更新库存信息
            Optional<Stockpile> stockpileOptional = stockpileRepository.findByProductId(productId);
            Stockpile stockpile;
            if (stockpileOptional.isPresent()) {
                stockpile = stockpileOptional.get();
                stockpile.setAmount(amount);
            } else {
                // 如果库存记录不存在，创建新的库存记录
                stockpile = new Stockpile();
                stockpile.setProductId(productId);
                stockpile.setAmount(amount);
                stockpile.setFrozen(0);
            }

            // 保存库存更新
            stockpileRepository.save(stockpile);
            return Response.buildSuccess("库存更新成功");

        } catch (Exception e) {
            return Response.buildFailure("更新库存失败: " + e.getMessage(), "500");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response<Stockpile> getStock(Integer productId) {
        try {
            // 检查商品是否存在
            Product product = productRepository.findById(productId)
                    .orElse(null);
            if (product == null) {
                return Response.buildFailure("商品不存在", "400");
            }

            // 查询商品库存信息
            Optional<Stockpile> stockpile = stockpileRepository.findByProductId(productId);
            if (stockpile.isPresent()) {
                return Response.buildSuccess(stockpile.get());
            } else {
                return Response.buildFailure("库存信息不存在", "404");
            }
        } catch (Exception e) {
            return Response.buildFailure("获取库存信息失败: " + e.getMessage(), "500");
        }
    }
}