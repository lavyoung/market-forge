package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 活动奖品展示响应。
 * <p>
 * 这个对象是给前端活动抽奖页使用的。
 * 它不只包含奖品基础信息，还包含当前用户视角下的解锁状态。
 *
 * @param awardId            奖品标识
 * @param awardTitle         奖品标题
 * @param awardSubtitle      奖品副标题
 * @param sort               展示排序
 * @param awardRuleLockCount 奖品解锁所需抽奖次数
 * @param isAwardUnlock      当前用户是否已解锁
 * @param waitUnLockCount    距离解锁还差几次
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/01
 */
@Schema(name = "ActivityAwardResponse", description = "活动奖品展示信息")
public record ActivityAwardResponse(
        @Schema(description = "奖品标识", example = "108")
        Long awardId,

        @Schema(description = "奖品标题", example = "1等奖")
        String awardTitle,

        @Schema(description = "奖品副标题", example = "抽奖2次后解锁")
        String awardSubtitle,

        @Schema(description = "奖品展示排序，数值越小越靠前", example = "8")
        Integer sort,

        @Schema(description = "奖品次数锁规则，抽奖 N 次后解锁；未配置时为空", example = "2")
        Integer awardRuleLockCount,

        @Schema(description = "奖品是否已解锁", example = "false")
        Boolean isAwardUnlock,

        @Schema(description = "距离解锁还需抽奖次数", example = "2")
        Integer waitUnLockCount
) implements Serializable {

}
