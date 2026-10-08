package com.lavyoung.marketforge.domain.behavior.model.entity;

import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;

/**
 * 用户行为实体。
 *
 * <p>表示外部传入的一次用户行为。本章先用它承接“用户签到”：
 * userId 表示谁做了行为，behaviorType 表示做了什么行为，
 * outBusinessNo 表示外部业务幂等号，例如签到日期。</p>
 *
 * @param userId        用户标识
 * @param behaviorType  行为类型
 * @param outBusinessNo 外部业务幂等号
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
public record BehaviorEntity(
        String userId,
        BehaviorTypeVO behaviorType,
        String outBusinessNo
) {
}
