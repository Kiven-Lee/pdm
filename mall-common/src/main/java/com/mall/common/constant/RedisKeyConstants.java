package com.mall.common.constant;

/**
 * Redis Key 常量
 * <p>
 * 统一管理所有 Redis Key 的前缀，避免 key 冲突和硬编码。
 * 命名规范：{模块}:{业务}:{标识}
 * </p>
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {}

    // ===== 商品模块 =====
    /** 商品详情缓存，完整 key = PRODUCT_DETAIL + productId */
    public static final String PRODUCT_DETAIL = "product:detail:";
    /** 商品分类列表缓存 */
    public static final String PRODUCT_CATEGORY_LIST = "product:category:list";
    /** 商品库存，完整 key = PRODUCT_STOCK + productId */
    public static final String PRODUCT_STOCK = "product:stock:";
    /** 商品库存扣减分布式锁，完整 key = PRODUCT_STOCK_LOCK + productId */
    public static final String PRODUCT_STOCK_LOCK = "product:stock:lock:";

    // ===== 购物车模块 =====
    /** 购物车 Hash，完整 key = CART + userId，Hash field = skuId */
    public static final String CART = "cart:";

    // ===== 认证模块 =====
    /** 用户 Token 黑名单（退出登录后加入），完整 key = TOKEN_BLACKLIST + token */
    public static final String TOKEN_BLACKLIST = "auth:token:blacklist:";
    /** RefreshToken 存储，完整 key = REFRESH_TOKEN + userId */
    public static final String REFRESH_TOKEN = "auth:refresh:";

    // ===== 订单模块 =====
    /** 订单超时取消延迟队列（使用 ZSet 实现），score = 过期时间戳 */
    public static final String ORDER_TIMEOUT_ZSET = "order:timeout:zset";
}
