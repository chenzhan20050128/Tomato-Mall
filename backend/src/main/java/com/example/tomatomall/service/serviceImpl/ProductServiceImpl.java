package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dao.SpecificationRepository;
import com.example.tomatomall.dao.StockpileRepository;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Specification;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Product> getAllProducts() {
        return productRepository.findAllWithSpecifications();
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Integer id) {
        Product product = productRepository.findByIdWithSpecifications(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // 确保小数点后两位
        if (product.getPrice() != null) {
            product.setPrice(product.getPrice().setScale(2));
        }
        return product;
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        // 基本验证
        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            throw new RuntimeException("商品标题不能为空");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("商品价格必须大于0");
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

        return savedProduct;
    }

    @Override
    @Transactional
    public String updateProduct(Product product) {
        if (product.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }

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
        return "更新成功";
    }

    @Override
    @Transactional
    public String deleteProduct(Integer id) {
        try {
            // 1. 先检查商品是否存在
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));

            // 2. 清除规格关联
            specificationRepository.deleteByProductId(id);

            // 3. 删除库存记录
            stockpileRepository.deleteByProductId(id);

            // 4. 最后删除商品本身
            productRepository.delete(product);

            return "删除成功";
        } catch (Exception e) {
            throw new RuntimeException("删除商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String updateStock(Integer productId, Integer amount) {
        // 验证参数
        if (amount == null || amount < 0) {
            throw new RuntimeException("库存数量不能为负数");
        }

        // 检查商品是否存在
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

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
        return "库存更新成功";
    }

    @Override
    @Transactional(readOnly = true)
    public Stockpile getStock(Integer productId) {
        // 检查商品是否存在
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 查询商品库存信息
        Optional<Stockpile> stockpile = stockpileRepository.findByProductId(productId);
        if (stockpile.isPresent()) {
            return stockpile.get();
        } else {
            throw new RuntimeException("库存信息不存在");
        }
    }
}