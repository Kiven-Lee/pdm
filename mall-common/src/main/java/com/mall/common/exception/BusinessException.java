package com.mall.common.exception;

import com.mall.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 在 Service 层抛出此异常，由 GlobalExceptionHandler 统一捕获并返回标准响应。
 * 使用示例：
 *   throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
 *   throw new BusinessException(ResultCode.PARAM_ERROR, "商品ID不能为空");
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final Integer code;

    /**
     * 使用预定义错误码构造异常
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用预定义错误码 + 自定义消息构造异常
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 使用自定义码和消息构造异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
