package com.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共基础实体
 * <p>
 * 所有业务实体继承此类，自动获得：
 *   - 雪花算法主键（@TableId）
 *   - 创建时间自动填充（@TableField fill = INSERT）
 *   - 更新时间自动填充（@TableField fill = INSERT_UPDATE）
 *   - 逻辑删除字段（@TableLogic，0=未删除，1=已删除）
 * </p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键：使用雪花算法生成全局唯一 ID
     * IdType.ASSIGN_ID = MyBatis-Plus 内置雪花算法
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间：INSERT 时由 MyBatis-Plus 自动填充
     * 需配合 MetaObjectHandler 使用
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间：INSERT 和 UPDATE 时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0=正常，1=已删除
     * MyBatis-Plus 查询时自动追加 deleted=0 条件
     */
    @TableLogic
    private Integer deleted;
}
