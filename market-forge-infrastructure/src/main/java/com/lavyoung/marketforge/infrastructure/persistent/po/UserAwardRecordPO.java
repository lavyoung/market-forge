package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户中奖记录持久化对象。
 * <p>
 * 保存用户在抽奖活动中的中奖明细，用于发奖、补偿和对账查询。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("user_award_record")
public class UserAwardRecordPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户标识。
     */
    private String userId;

    /**
     * 活动标识。
     */
    private Long activityId;

    /**
     * 抽奖策略标识。
     */
    private Long strategyId;

    /**
     * 抽奖订单标识。
     */
    private String orderId;

    /**
     * 奖品标识。
     */
    private Long awardId;

    /**
     * 奖品标题。
     */
    private String awardTitle;

    /**
     * 中奖时间。
     */
    private LocalDateTime awardTime;

    /**
     * 发奖状态。
     */
    private String awardState;
}
