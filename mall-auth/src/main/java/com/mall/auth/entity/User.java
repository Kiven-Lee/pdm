package com.mall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 * 对应数据库表 mall_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_user")
public class User extends BaseEntity {

    /** 用户名（唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /**
     * 用户状态：0=正常，1=禁用
     * 禁用用户无法登录
     */
    private Integer status;
}
