package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 抽奖活动持久化对象。
 * <p>
 * 保存活动的时间范围、库存、参与次数配置、抽奖策略及启用状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity")
public class ActivityPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 抽奖活动业务标识。
     */
    private Long activityId;

    /**
     * 活动名称。
     */
    private String activityName;

    /**
     * 活动说明。
     */
    private String activityDesc;

    /**
     * 活动开始时间。
     */
    private LocalDateTime beginDateTime;

    /**
     * 活动结束时间。
     */
    private LocalDateTime endDateTime;

    /**
     * 活动库存总量。
     */
    private Integer stockCount;

    /**
     * 活动剩余库存。
     */
    private Integer stockCountSurplus;

    /**
     * 活动参与次数配置标识。
     */
    private Long activityCountId;

    /**
     * 活动关联的抽奖策略标识。
     */
    private Long strategyId;

    /**
     * 活动状态。
     */
    private String state;
}
