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
 * 抽奖活动订单持久化对象。
 * <p>
 * 记录用户领取的活动抽奖次数订单，并保存订单的使用状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_order")
public class ActivityOrderPO extends BasePO {

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
     * 用户下单的商品 SKU。
     */
    private Long sku;

    /**
     * 下单时的活动名称快照。
     */
    private String activityName;

    /**
     * 活动关联的抽奖策略标识。
     */
    private Long strategyId;

    /**
     * 业务订单号。
     */
    private String orderId;

    /**
     * 业务下单时间。
     */
    private LocalDateTime orderTime;

    /**
     * 订单授予的总抽奖次数。
     */
    private Integer totalCount;

    /**
     * 订单授予的日抽奖次数。
     */
    private Integer dayCount;

    /**
     * 订单授予的月抽奖次数。
     */
    private Integer monthCount;

    /**
     * 订单状态，例如 {@code not_used}、{@code used}、{@code expire} 或 {@code complete}。
     */
    private String state;

    /**
     * 业务幂等ID
     */
    private String outBusinessNo;
}
