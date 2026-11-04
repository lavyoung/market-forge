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
 *
 * <p>对应 {@code raffle_activity} 表，保存活动名称、有效期、参与次数配置、
 * 抽奖策略、奖品解锁范围和生命周期状态。</p>
 *
 * <p>活动定义不保存库存。额度商品库存由 {@code raffle_activity_sku} 维护，
 * 策略奖品库存由 {@code strategy_award} 维护。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
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
     * 活动参与次数配置标识。
     */
    private Long activityCountId;

    /**
     * 活动关联的抽奖策略标识。
     */
    private Long strategyId;

    /**
     * 奖品次数锁统计范围。
     *
     * <p>数据库保存 {@code day} 或 {@code total}，转换为领域实体时由
     * Assembler 解析为 {@code AwardUnlockScopeVO}。</p>
     */
    private String awardUnlockScope;

    /**
     * 活动生命周期状态。
     */
    private String state;

    /**
     * 乐观锁版本。
     */
    private Integer version;

    /**
     * 创建人标识。
     */
    private String createdBy;

    /**
     * 最后修改人标识。
     */
    private String updatedBy;
}
