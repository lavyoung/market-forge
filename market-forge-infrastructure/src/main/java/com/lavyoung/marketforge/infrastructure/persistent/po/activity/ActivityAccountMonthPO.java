package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户抽奖活动账户月次数持久化对象。
 * <p>
 * 保存用户在指定活动、指定月份下的可用抽奖次数，并按用户标识分库。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_account_month")
public class ActivityAccountMonthPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户标识，同时作为分库路由键。
     */
    private String userId;

    /**
     * 抽奖活动标识。
     */
    private Long activityId;

    /**
     * 每月可用次数。
     */
    private Integer monthCount;

    /**
     * 当月剩余次数。
     */
    private Integer monthCountSurplus;

    /**
     * 月份，格式为 yyyy-MM。
     */
    private String month;

    /**
     * 乐观锁版本号。
     */
    private Integer version;
}
