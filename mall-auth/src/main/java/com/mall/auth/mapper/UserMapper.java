package com.mall.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 继承 BaseMapper 获得基础 CRUD 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
