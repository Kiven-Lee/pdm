package com.mall.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.cart.dto.CartItem;
import com.mall.common.constant.RedisKeyConstants;
import com.mall.common.exception.BusinessException;
import com.mall.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车服务
 * <p>
 * 购物车数据完全存储在 Redis Hash 中：
 *   - key   = "cart:{userId}"
 *   - field = "{productId}"
 *   - value = CartItem（JSON 序列化）
 *
 * 优点：
 *   - 读写速度快（O(1) 复杂度）
 *   - 天然支持按商品 ID 查找/更新/删除
 *   - 无需数据库，减少 IO
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** 每个用户购物车最大商品种类数 */
    @Value("${cart.max-item-count:50}")
    private int maxItemCount;

    /** 购物车过期时间（秒） */
    @Value("${cart.expire:2592000}")
    private long cartExpire;

    /**
     * 加入购物车
     * <p>
     * 如果商品已在购物车中，则累加数量；否则新增条目。
     * </p>
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param productName 商品名称
     * @param mainImage 商品主图
     * @param price     商品价格
     * @param quantity  加购数量
     */
    public void addToCart(Long userId, Long productId, String productName,
                          String mainImage, BigDecimal price, Integer quantity) {
        String cartKey = RedisKeyConstants.CART + userId;
        String fieldKey = String.valueOf(productId);

        // 检查购物车商品种类数量限制
        Long cartSize = redisTemplate.opsForHash().size(cartKey);
        boolean exists = Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(cartKey, fieldKey));
        if (!exists && cartSize >= maxItemCount) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "购物车最多添加 " + maxItemCount + " 种商品");
        }

        if (exists) {
            // 商品已在购物车，累加数量
            CartItem existItem = getCartItem(cartKey, fieldKey);
            if (existItem != null) {
                existItem.setQuantity(existItem.getQuantity() + quantity);
                saveCartItem(cartKey, fieldKey, existItem);
            }
        } else {
            // 新增购物车条目
            CartItem item = new CartItem(productId, productName, mainImage, price, quantity, true);
            saveCartItem(cartKey, fieldKey, item);
        }

        // 每次操作后刷新过期时间（用户活跃则购物车不过期）
        redisTemplate.expire(cartKey, cartExpire, TimeUnit.SECONDS);
        log.debug("加入购物车: userId={}, productId={}, quantity={}", userId, productId, quantity);
    }

    /**
     * 获取购物车列表
     *
     * @param userId 用户 ID
     * @return 购物车条目列表
     */
    public List<CartItem> getCartList(Long userId) {
        String cartKey = RedisKeyConstants.CART + userId;
        Map<Object, Object> cartMap = redisTemplate.opsForHash().entries(cartKey);

        List<CartItem> items = new ArrayList<>();
        for (Object value : cartMap.values()) {
            CartItem item = convertToCartItem(value);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * 修改购物车商品数量
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param quantity  新的数量（必须 >= 1）
     */
    public void updateQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "数量不能小于 1");
        }

        String cartKey = RedisKeyConstants.CART + userId;
        String fieldKey = String.valueOf(productId);

        CartItem item = getCartItem(cartKey, fieldKey);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "购物车中不存在该商品");
        }

        item.setQuantity(quantity);
        saveCartItem(cartKey, fieldKey, item);
        log.debug("修改购物车数量: userId={}, productId={}, quantity={}", userId, productId, quantity);
    }

    /**
     * 删除购物车中的商品
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     */
    public void removeFromCart(Long userId, Long productId) {
        String cartKey = RedisKeyConstants.CART + userId;
        // Hash 删除指定 field
        redisTemplate.opsForHash().delete(cartKey, String.valueOf(productId));
        log.debug("从购物车删除: userId={}, productId={}", userId, productId);
    }

    /**
     * 清空购物车（下单成功后调用）
     *
     * @param userId 用户 ID
     */
    public void clearCart(Long userId) {
        String cartKey = RedisKeyConstants.CART + userId;
        redisTemplate.delete(cartKey);
        log.debug("清空购物车: userId={}", userId);
    }

    /**
     * 更新商品选中状态
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param checked   是否选中
     */
    public void updateChecked(Long userId, Long productId, Boolean checked) {
        String cartKey = RedisKeyConstants.CART + userId;
        String fieldKey = String.valueOf(productId);

        CartItem item = getCartItem(cartKey, fieldKey);
        if (item != null) {
            item.setChecked(checked);
            saveCartItem(cartKey, fieldKey, item);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 Redis Hash 中获取购物车条目并反序列化
     */
    private CartItem getCartItem(String cartKey, String fieldKey) {
        Object value = redisTemplate.opsForHash().get(cartKey, fieldKey);
        return convertToCartItem(value);
    }

    /**
     * 将 CartItem 序列化后存入 Redis Hash
     */
    private void saveCartItem(String cartKey, String fieldKey, CartItem item) {
        try {
            String json = objectMapper.writeValueAsString(item);
            redisTemplate.opsForHash().put(cartKey, fieldKey, json);
        } catch (Exception e) {
            log.error("购物车数据序列化失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
    }

    /**
     * 将 Redis 中的值转换为 CartItem 对象
     * Redis 中存储的是 JSON 字符串
     */
    private CartItem convertToCartItem(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String) {
                return objectMapper.readValue((String) value, CartItem.class);
            }
            // 如果 RedisTemplate 配置了 JSON 序列化，value 可能已经是 CartItem
            return objectMapper.convertValue(value, CartItem.class);
        } catch (Exception e) {
            log.error("购物车数据反序列化失败: {}", value, e);
            return null;
        }
    }
}
