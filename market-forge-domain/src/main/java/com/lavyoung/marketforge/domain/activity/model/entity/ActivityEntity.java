package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 抽奖活动运行时领域实体。
 *
 * <p>该实体描述活动参与和抽奖过程中需要使用的活动配置，包括活动有效期、
 * 生命周期状态、次数配置和抽奖策略。活动自身不持有库存，原因是实际库存分别
 * 归属于活动 SKU 和策略奖品：</p>
 *
 * <ul>
 *     <li>活动 SKU 库存控制还能出售或兑换多少份活动参与额度。</li>
 *     <li>策略奖品库存控制某个奖品还能被抽中多少次。</li>
 * </ul>
 *
 * <p>将库存继续放在活动实体中会形成第三份无法可靠同步的库存状态，因此活动
 * 运行时只负责提供活动规则，不承担库存扣减。</p>
 *
 * @param activityId       抽奖活动标识
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    活动开始时间
 * @param endDateTime      活动结束时间
 * @param activityCountId  活动参与次数配置标识
 * @param strategyId       活动关联的抽奖策略标识
 * @param awardUnlockScope 奖品次数锁的统计范围
 * @param state            活动状态
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Builder
public record ActivityEntity(
        Long activityId,
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        Long activityCountId,
        Long strategyId,
        AwardUnlockScopeVO awardUnlockScope,
        String state
) {
}
