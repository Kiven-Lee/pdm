package com.mall.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 物流轨迹实体
 * 对应数据库表 logistics_track
 * 记录物流的每一个节点信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_track")
public class LogisticsTrack extends BaseEntity {

    /** 所属物流单 ID */
    private Long logisticsId;

    /** 轨迹位置（如：上海转运中心） */
    private String location;

    /** 轨迹描述（如：快件已到达上海转运中心） */
    private String remark;

    /** 轨迹时间 */
    private LocalDateTime trackTime;

    /**
     * 轨迹类型：
     *   1 = 揽收
     *   2 = 转运
     *   3 = 派送
     *   4 = 签收
     *   5 = 异常
     */
    private Integer trackType;
}
