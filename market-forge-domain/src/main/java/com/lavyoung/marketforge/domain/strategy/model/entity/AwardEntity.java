package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 奖品基础信息。
 *
 * @param awardId     奖品标识
 * @param awardKey    奖品业务键
 * @param awardConfig 奖品发放配置
 * @param awardDesc   奖品描述
 * @param createTime  创建时间
 * @param updateTime  更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/08/31
 */
@Builder
public record AwardEntity(
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardDesc,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

}
