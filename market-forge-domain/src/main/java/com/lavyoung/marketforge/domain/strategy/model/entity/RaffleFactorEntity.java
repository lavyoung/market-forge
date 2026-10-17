package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 执行抽奖所需的领域输入因子。
 *
 * <p>该对象贯穿策略责任链和规则树，活动抽奖会携带完整的
 * 奖品解锁上下文；非活动抽奖允许活动上下文字段为空。</p>
 *
 * @param userId           参与抽奖的用户标识
 * @param strategyId       本次抽奖使用的策略标识
 * @param awardId          随机命中的候选奖品标识；抽奖前可为空
 * @param activityId       当前活动标识；非活动抽奖时可为空
 * @param awardUnlockScope 奖品解锁次数统计范围；非活动抽奖时可为空
 * @param statisticsDate   解锁统计业务日期；非活动抽奖时可为空
 * @param endDateTime      活动结束时间；非活动抽奖时可为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RaffleFactorEntity(
        String userId,
        Long strategyId,
        Long awardId,
        Long activityId,
        AwardUnlockScopeVO awardUnlockScope,
        LocalDate statisticsDate,
        LocalDateTime endDateTime
) {

    /**
     * 判断当前抽奖因子是否携带完整活动上下文。
     *
     * @return 包含活动标识、解锁范围和统计日期时返回 {@code true}
     */
    public boolean hasActivityContext() {
        return activityId != null && awardUnlockScope != null && statisticsDate != null;
    }

    /**
     * 复制当前抽奖因子并写入责任链随机命中的候选奖品。
     *
     * <p>record 为不可变对象，因此规则树阶段通过创建副本携带奖品标识，
     * 不在原对象上维护可变状态。</p>
     *
     * @param selectedAwardId 责任链随机命中的奖品标识
     * @return 携带候选奖品标识的新抽奖因子
     * @throws NullPointerException 候选奖品标识为空时抛出
     */
    public RaffleFactorEntity withAwardId(Long selectedAwardId) {
        return new RaffleFactorEntity(
                userId,
                strategyId,
                Objects.requireNonNull(selectedAwardId, "selectedAwardId must not be null"),
                activityId,
                awardUnlockScope,
                statisticsDate,
                endDateTime
        );
    }
}
