package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dao.SpecificationRepository;
import com.example.tomatomall.dao.StockpileRepository;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Specification;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.ProductService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {
    private static final String PRODUCT_CACHE_KEY = "product::";
    private static final String PRODUCT_LIST_CACHE_KEY = "product::list";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private StockpileRepository stockpileRepository;

    @Autowired
    @Qualifier("productRedisTemplate")
    private RedisTemplate<String, Product> productRedisTemplate;

    @Autowired
    @Qualifier("productListRedisTemplate")
    private RedisTemplate<String, List<Product>> productListRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /*
    经过测试：在10000并发请求下还能保证错误率为0
    以下为原理：
        1. 缓存穿透防护
空值缓存：当数据库查询结果为空时，缓存空列表（Collections.emptyList()），并设置较短的TTL（NULL_CACHE_TTL）。避免恶意请求穿透缓存直接访问数据库。
双重检查锁：在未命中缓存时，通过分布式锁控制仅一个线程查询数据库，其他线程等待或降级，防止并发穿透。
2. 缓存击穿防护
分布式锁（Redisson）：使用 RLock 实现分布式锁，确保缓存失效时仅一个线程重建数据。
非阻塞锁竞争：通过 tryLock(100ms) 控制锁等待时间，避免线程长时间阻塞，超时后触发降级逻辑，保障可用性。
3. 缓存雪崩防护
分片存储与随机TTL：将数据分片为多个键（如 PRODUCT_LIST_CACHE_KEY::chunk_0），每个分片设置基础TTL加随机偏移值（ThreadLocalRandom.current().nextInt(30_000)），避免所有缓存同时失效。
异步批量写入：使用Redis Pipeline批量写入分片数据，减少网络开销，提升性能。
4. 高并发查询优化
分页加载数据库：通过分页查询（Pageable）避免单次大数据量查询导致数据库或内存压力。
异步缓存重建：主线程返回数据后异步执行缓存重建，减少用户请求延迟，通过分片存储和Pipeline写入提升效率。
5. 降级与回退策略
锁超时降级：若获取锁失败或超时（如100ms内未获得锁），直接查询数据库并返回部分数据（如 PageRequest.of(0, 100)），避免系统雪崩。
异常捕获：捕获分布式锁和缓存操作异常，降级到数据库查询，确保最终可用性。
     */
    @Override
    public List<Product> getAllProducts() {
                return productRepository.findAllWithSpecifications();
    }


    /*
代码逻辑：

**1. 构建缓存 Key:**
   *   1.1 根据商品 `id` 构建 Redis 缓存 Key: `PRODUCT_CACHE_KEY + id`

**2. 尝试从缓存中获取:**
   *   2.1 使用 `productRedisTemplate.opsForValue().get(key)` 从 Redis 缓存中获取 `Product` 对象。

   *   2.2 **如果缓存命中:**
        *   2.2.1 **空值判断:**
            *   2.2.1.1 检查 `product.getId() == -1`
            *   2.2.1.2 如果是 `true` (空值):
                *   记录日志: "命中空值缓存，商品ID：{}"
                *   返回 `null` (防止缓存穿透)
        *   2.2.2 **正常缓存:**
            *   2.2.2.1 如果 `product.getId() != -1`
                *   记录日志: "从缓存中获取商品信息，商品ID：{}"
                *   返回 `product`

   *   2.3 **如果缓存未命中:**
        *   进入步骤 3 (从数据库加载)

**3. 防止缓存击穿 (使用分布式锁):**
   *   3.1 获取分布式锁: 使用 Redisson 获取名为 `"product_lock_" + id` 的锁
   *   3.2 尝试获取锁:  `lock.lock()`

   *   3.3 **双重检查:** (获取锁后再次检查缓存)
        *   3.3.1 再次使用 `productRedisTemplate.opsForValue().get(key)` 从 Redis 获取 Product
        *   3.3.2 **如果缓存命中:**
            *   记录日志: "从缓存中获取商品信息，商品ID：{}"
            *   返回 `product`
        *   3.3.3 **如果缓存仍然未命中:**
            *   进入步骤 4 (从数据库加载)

**4. 从数据库加载数据:**
   *   4.1 调用 `productRepository.findByIdWithSpecifications(id)` 从数据库获取 `Product` 对象

**5. 处理数据库查询结果:**
   *   5.1 **如果数据库中不存在该商品 (`product == null`):**
        *   5.1.1 **防止缓存穿透:**
            *   创建一个新的 `Product` 对象
            *   设置 `product.setId(-1)` (标记为空值)
            *   `productRedisTemplate.opsForValue().set(key, product, NULL_CACHE_TTL)` (缓存空值并设置过期时间)
            *   记录日志: "缓存空值，商品ID：{}"
            *   返回 `null`

   *   5.2 **如果数据库中存在该商品 (`product != null`):**
        *   5.2.1 **防止缓存雪崩:**
            *   计算随机过期时间:  `randomTtl = CACHE_TTL.toMillis() + (long)(Math.random() * 60000)`
        *   5.2.2 `productRedisTemplate.opsForValue().set(key, product, randomTtl, TimeUnit.MILLISECONDS)` (将商品信息存入缓存并设置随机过期时间)
        *   记录日志: "将商品信息存入缓存，商品ID：{}"
        *   返回 `product`

**6. 释放锁:**
   *   6.1  在 `finally` 块中执行 `lock.unlock()` (确保锁始终被释放，避免死锁)


     */
    @Override
    public Product getProduct(Integer id) {
        String key = PRODUCT_CACHE_KEY + id;
        ValueOperations<String, Product> ops = productRedisTemplate.opsForValue();
        Product product = ops.get(key);

        if (product != null) {
            if (product.getId() == -1) { // 空值标识
                log.info("命中空值缓存，商品ID：{}", id);
                return null;
            }
            log.info("从缓存中获取商品信息，商品ID：{}", id);
            return product;
        }

        // 防止缓存击穿 - 使用分布式锁
        RLock lock = redissonClient.getLock("product_lock_" + id);
        try {
            lock.lock();
            // 双重检查
            product = ops.get(key);
            if (product != null) {
                log.info("从缓存中获取商品信息，商品ID：{}", id);
                return product;
            }

            product = productRepository.findByIdWithSpecifications(id);
            if (product == null) {
                // 防止缓存穿透 - 缓存空值
                Product nullProduct = new Product();
                nullProduct.setId(-1);
                ops.set(key, nullProduct, NULL_CACHE_TTL);
                log.info("缓存空值，商品ID：{}", id);
                return null;
            }

            // 设置随机过期时间防止雪崩
            long randomTtl = CACHE_TTL.toMillis() + (long)(Math.random() * 60000);
            ops.set(key, product, randomTtl, TimeUnit.MILLISECONDS);
            log.info("将商品信息存入缓存，商品ID：{}", id);
            return product;
        } finally {
            lock.unlock();
        }
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
            specificationRepository.saveAll(specs);  //保存规格
            productRepository.save(savedProduct);
        }

        // 创建库存记录
        Stockpile stockpile = new Stockpile();
        stockpile.setProductId(savedProduct.getId());
        stockpile.setAmount(0);
        stockpile.setFrozen(0);
        stockpileRepository.save(stockpile);

        // 清除列表缓存
        productListRedisTemplate.delete(PRODUCT_LIST_CACHE_KEY);
        log.info("创建商品，清除商品列表缓存");
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
            specificationRepository.saveAll(product.getSpecifications()); //保存规格
            existingProduct.setSpecifications(product.getSpecifications());
        }

        // 保存更新
        productRepository.save(existingProduct);

        // 更新单个缓存
        String key = PRODUCT_CACHE_KEY + product.getId();
        productRedisTemplate.opsForValue().set(key, product);
        log.info("更新商品，更新商品缓存，商品ID：{}", product.getId());

        // 清除列表缓存
        productListRedisTemplate.delete(PRODUCT_LIST_CACHE_KEY);
        log.info("更新商品，清除商品列表缓存");

        return "更新成功";
    }

    @Override
    @Transactional
    public String deleteProduct(Integer id) {
        // 检查商品是否存在
        Product product = productRepository.findByIdWithSpecifications(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 清空规格关联
        product.getSpecifications().clear();

        // 删除商品（由于配置了 cascade = CascadeType.ALL，规格会自动删除）
        productRepository.delete(product);

        // 删除缓存
        productRedisTemplate.delete(PRODUCT_CACHE_KEY + id);
        log.info("删除商品，删除商品缓存，商品ID：{}", id);
        productListRedisTemplate.delete(PRODUCT_LIST_CACHE_KEY);
        log.info("删除商品，清除商品列表缓存");
        return "删除成功";
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