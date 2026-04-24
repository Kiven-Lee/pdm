package com.mall.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 配合 BaseEntity 中的 @TableField(fill = FieldFill.INSERT/INSERT_UPDATE) 使用，
 * 在 INSERT 和 UPDATE 时自动填充 createTime、updateTime 字段，
 * 无需在业务代码中手动设置时间。
 * </p>
 */
@Slf4j
@Component
public class MybatisPlusMetaHandler implements MetaObjectHandler {

    /**
     * 执行 INSERT 时自动填充
     * 填充 createTime 和 updateTime 为当前时间
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // strictInsertFill：严格模式，只有字段值为 null 时才填充
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        // 逻辑删除字段默认为 0（未删除）
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    /**
     * 执行 UPDATE 时自动填充
     * 只更新 updateTime，不修改 createTime
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
