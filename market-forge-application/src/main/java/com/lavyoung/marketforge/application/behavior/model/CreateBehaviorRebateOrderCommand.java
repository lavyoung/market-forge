package com.lavyoung.marketforge.application.behavior.model;

import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;

/**
 * 创建用户行为返利订单命令。
 *
 * <p>应用层使用该命令承接外部入口传入的用户行为信息，
 * 再转换为领域层 {@code BehaviorEntity} 执行返利入账。</p>
 *
 * @param userId        用户标识
 * @param behaviorType  行为类型
 * @param outBusinessNo 外部业务幂等号，例如签到日期
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
public record CreateBehaviorRebateOrderCommand(
        String userId,
        BehaviorTypeVO behaviorType,
        String outBusinessNo
) {
}
