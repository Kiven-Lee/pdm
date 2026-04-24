package com.mall.common.result;

import lombok.Data;

/**
 * 统一 API 响应体
 * <p>
 * 所有接口返回值均包装为此格式，前端通过 code 判断业务是否成功：
 *   200 = 成功
 *   非 200 = 业务失败（具体含义见 ResultCode 枚举）
 * </p>
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {

    /** 业务状态码，200 表示成功 */
    private Integer code;

    /** 提示信息，成功时为 "success"，失败时为具体错误描述 */
    private String message;

    /** 响应数据，失败时为 null */
    private T data;

    // ===== 私有构造，强制使用静态工厂方法 =====

    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（携带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（无数据，如新增/删除操作）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应（使用预定义错误码）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误信息）
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断当前响应是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
