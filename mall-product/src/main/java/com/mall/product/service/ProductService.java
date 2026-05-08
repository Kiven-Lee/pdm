package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.constant.RedisKeyConstants;
import com.mall.common.exception.BusinessException;
import com.mall.common.result.ResultCode;
import com.mall.product.entity.Category;
import com.mall.product.entity.Product;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mq.ProductKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务
 * <p>
 * 核心功能：
 *   1. 商品 CRUD（管理端）
 *   2. 商品列表查询（带分页和分类筛选）
 *   3. 商品详情查询（Redis 缓存 + 浏览日志）
 *   4. 库存扣减（Redis 分布式锁 + 数据库乐观锁双重保障）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductKafkaProducer kafkaProducer;

    /** 商品详情缓存过期时间（秒） */
    @Value("${product.cache.detail-expire:1800}")
    private long detailCacheExpire;

    /** 分类列表缓存过期时间（秒） */
    @Value("${product.cache.category-expire:3600}")
    private long categoryCacheExpire;

    /** 库存分布式锁超时时间（秒） */
    @Value("${product.stock-lock-timeout:10}")
    private long stockLockTimeout;

    /**
     * 分页查询商品列表
     *
     * @param page       页码（从 1 开始）
     * @param size       每页数量
     * @param categoryId 分类 ID（null 表示不过滤）
     * @param keyword    搜索关键词（null 表示不过滤）
     * @return 分页结果
     */
    public Page<Product> listProducts(int page, int size, Long categoryId, String keyword) {
        Page<Product> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                // 只查询上架商品
                .eq(Product::getStatus, 1)
                // 按分类过滤（categoryId 不为 null 时才加此条件）
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                // 关键词模糊搜索商品名称
                .like(keyword != null && !keyword.isEmpty(), Product::getName, keyword)
                // 按创建时间倒序
                .orderByDesc(Product::getCreateTime);

        return productMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 获取商品详情（带 Redis 缓存）
     * <p>
     * 缓存策略：Cache-Aside（旁路缓存）
     *   1. 先查 Redis
     *   2. Redis 未命中则查 MySQL，并写入 Redis
     *   3. 商品更新时主动删除 Redis 缓存（Cache Invalidation）
     * </p>
     *
     * @param productId 商品 ID
     * @param userId    当前用户 ID（用于记录浏览日志，可为 null）
     */
    public Product getProductDetail(Long productId, Long userId) {
        String cacheKey = RedisKeyConstants.PRODUCT_DETAIL + productId;

        // 1. 先从 Redis 缓存中查询
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("商品详情缓存命中: productId={}", productId);
            // 异步发送浏览日志（不影响响应速度）
            kafkaProducer.sendProductViewLog(productId, userId);
            return (Product) cached;
        }

        // 2. 缓存未命中，查询数据库
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.PRODUCT_OFFLINE);
        }

        // 3. 写入 Redis 缓存，设置过期时间防止缓存永久占用内存
        redisTemplate.opsForValue().set(cacheKey, product, detailCacheExpire, TimeUnit.SECONDS);
        log.debug("商品详情写入缓存: productId={}", productId);

        // 4. 异步发送浏览日志到 Kafka
        kafkaProducer.sendProductViewLog(productId, userId);

        return product;
    }

    /**
     * 获取所有分类列表（带 Redis 缓存）
     * 分类数据变化频率低，缓存时间可以设置较长
     */
    @SuppressWarnings("unchecked")
    public List<Category> listCategories() {
        String cacheKey = RedisKeyConstants.PRODUCT_CATEGORY_LIST;

        // 先查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<Category>) cached;
        }

        // 查数据库，按 sort 字段排序
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)
        );

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, categories, categoryCacheExpire, TimeUnit.SECONDS);
        return categories;
    }

    /**
     * 新增商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(Product product) {
        // 默认上架状态
        product.setStatus(1);
        product.setSales(0);
        productMapper.insert(product);
        log.info("商品新增成功: name={}", product.getName());
    }

    /**
     * 更新商品信息
     * 更新后删除 Redis 缓存，下次查询时重新加载
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Product product) {
        productMapper.updateById(product);
        // 主动删除缓存（Cache Invalidation），避免脏数据
        String cacheKey = RedisKeyConstants.PRODUCT_DETAIL + product.getId();
        redisTemplate.delete(cacheKey);
        log.info("商品更新成功，缓存已清除: productId={}", product.getId());
    }

    /**
     * 扣减库存（使用 Redis 分布式锁 + 数据库乐观锁）
     * <p>
     * 双重保障策略：
     *   1. Redis 分布式锁：防止同一商品的并发扣减请求同时进入数据库
     *   2. 数据库乐观锁（SQL 中 stock >= quantity）：最终兜底，防止超卖
     * </p>
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @throws BusinessException 库存不足时抛出
     */
    public void deductStock(Long productId, Integer quantity) {
        // 分布式锁 key，格式：product:stock:lock:{productId}
        String lockKey = RedisKeyConstants.PRODUCT_STOCK_LOCK + productId;

        // 使用 Redis SETNX 实现分布式锁
        // setIfAbsent = SET key value NX EX timeout（原子操作）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", stockLockTimeout, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            // 获取锁失败，说明有其他请求正在处理此商品的库存
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
        }

        try {
            // 执行数据库库存扣减（SQL 中包含 stock >= quantity 条件）
            int affected = productMapper.deductStock(productId, quantity);
            if (affected == 0) {
                // 影响行数为 0，说明库存不足
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }
            log.info("库存扣减成功: productId={}, quantity={}", productId, quantity);
        } finally {
            // 无论成功还是失败，都必须释放锁
            // 使用 finally 确保锁一定被释放，防止死锁
            redisTemplate.delete(lockKey);
        }
    }
}
