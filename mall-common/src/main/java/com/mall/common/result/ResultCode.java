package com.mall.common.result;

import lombok.Getter;

/**
 * 业务状态码枚举
 * <p>
 * 规范：
 *   1xx  系统级错误
 *   2xx  成功
 *   4xx  客户端错误（参数、权限等）
 *   5xx  服务端错误
 *   6xx  业务错误（库存不足、订单状态异常等）
 * </p>
 */
@Getter
public enum ResultCode {

    // ===== 成功 =====
    SUCCESS(200, "success"),

    // ===== 客户端错误 =====
    /** 请求参数校验失败 */
    PARAM_ERROR(400, "请求参数错误"),
    /** 未登录或 Token 失效 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** 无权限访问 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    // ===== 服务端错误 =====
    /** 系统内部错误 */
    INTERNAL_ERROR(500, "系统内部错误"),

    // ===== 业务错误 =====
    /** 用户名或密码错误 */
    LOGIN_FAIL(600, "用户名或密码错误"),
    /** 用户名已存在 */
    USER_EXISTS(601, "用户名已存在"),
    /** 商品库存不足 */
    STOCK_NOT_ENOUGH(610, "商品库存不足"),
    /** 商品已下架 */
    PRODUCT_OFFLINE(611, "商品已下架或不存在"),
    /** 购物车为空 */
    CART_EMPTY(620, "购物车为空"),
    /** 订单状态异常，不允许此操作 */
    ORDER_STATUS_ERROR(630, "订单状态异常"),
    /** 订单不存在 */
    ORDER_NOT_FOUND(631, "订单不存在");

    /** HTTP 状态码或业务码 */
    private final Integer code;
    /** 描述信息 */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
