package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户抽奖活动账户流水持久化对象。
 * <p>
 * 记录活动领取、购买、兑换或赠送产生的次数变动，并按用户标识分库。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_account_flow")
public class ActivityAccountFlowPO extends BasePO {

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
     * 本次变动的总次数。
     */
    private Integer totalCount;

    /**
     * 本次变动的日次数。
     */
    private Integer dayCount;

    /**
     * 本次变动的月次数。
     */
    private Integer monthCount;

    /**
     * 唯一流水标识。
     */
    private String flowId;

    /**
     * 流水来源渠道，例如 {@code activity}、{@code sale}、{@code redeem} 或 {@code free}。
     */
    private String flowChannel;

    /**
     * 外部业务标识，用于幂等校验。
     */
    private String bizId;
}
