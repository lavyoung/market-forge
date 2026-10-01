package com.lavyoung.marketforge.application.activity.model;

/**
 * 活动奖品展示结果。
 * <p>
 * 该结果面向活动抽奖页，不只表达奖品静态配置，还表达当前用户视角下的次数锁解锁状态。
 *
 * @param awardId            奖品标识
 * @param awardTitle         奖品标题
 * @param awardSubtitle      奖品副标题
 * @param sort               展示排序
 * @param awardRuleLockCount 奖品解锁所需抽奖次数；未配置次数锁时为空
 * @param isAwardUnlock      当前用户是否已满足次数锁条件
 * @param waitUnLockCount    当前用户距离解锁还需要参与的次数；已解锁时为 {@code 0}
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
        Boolean isAwardUnlock,
        Integer waitUnLockCount
) {
}
