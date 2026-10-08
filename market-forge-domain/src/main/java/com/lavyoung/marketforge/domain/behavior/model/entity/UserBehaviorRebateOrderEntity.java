package com.lavyoung.marketforge.domain.behavior.model.entity;

import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;

import java.time.LocalDateTime;

/**
 * 用户行为返利订单实体。
 *
 * <p>配置表示“应该给什么”，订单表示“已经为某个用户生成了一笔具体返利”。
 * 订单里的 bizId 是后续发放侧的幂等标识，防止 MQ 重复消费导致重复发放。</p>
 *
 * @param userId        用户标识
 * @param orderId       返利订单号(系统内部)
 * @param behaviorType  行为类型
 * @param rebateType    返利类型
 * @param rebateConfig  返利配置值
 * @param bizId         下游发放幂等标识
 * @param outBusinessNo 外部业务幂等号
 * @param orderTime     订单创建时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
public record UserBehaviorRebateOrderEntity(
        String userId,
        String orderId,
        BehaviorTypeVO behaviorType,
        RebateTypeVO rebateType,
        String rebateConfig,
        String bizId,
        String outBusinessNo,
        LocalDateTime orderTime
) {
}
