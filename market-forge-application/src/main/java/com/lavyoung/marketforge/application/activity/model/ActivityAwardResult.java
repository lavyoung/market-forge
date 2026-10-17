package com.lavyoung.marketforge.application.activity.model;

/**
 * 活动奖品展示结果。
 *
 * <p>该结果面向活动抽奖页，同时包含奖品静态信息、次数锁状态
 * 和权重奖池状态。</p>
 *
 * @param awardId              奖品标识
 * @param awardTitle           奖品标题
 * @param awardSubtitle        奖品副标题
 * @param sort                 展示排序
 * @param awardRuleLockCount   奖品解锁所需次数；未配置时为空
 * @param awardUnlockScope     解锁次数统计范围，取值为 day 或 total
 * @param isAwardUnlock        当前用户是否满足次数锁条件
 * @param waitUnLockCount      距离次数锁解锁还需要完成的次数
 * @param awardRuleWeightCount 进入权重奖池所需累计抽奖次数
 * @param isAwardWeightUnlock  当前用户是否满足权重奖池门槛
 * @param waitAwardWeightCount 距离权重奖池门槛还需要完成的次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/01
 */
public record ActivityAwardResult(
        Long awardId,
        String awardTitle,
        String awardSubtitle,
        Integer sort,
        Integer awardRuleLockCount,
        String awardUnlockScope,
        Boolean isAwardUnlock,
        Integer waitUnLockCount,
        Integer awardRuleWeightCount,
        Boolean isAwardWeightUnlock,
        Integer waitAwardWeightCount
) {
}
