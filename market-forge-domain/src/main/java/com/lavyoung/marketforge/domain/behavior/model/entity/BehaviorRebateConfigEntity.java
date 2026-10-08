package com.lavyoung.marketforge.domain.behavior.model.entity;

import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;

/**
 * 行为返利配置实体。
 *
 * <p>表示某种用户行为可以获得什么返利。例如：
 * SIGN -> SKU -> 9011，
 * SIGN -> INTEGRAL -> 10。</p>
 *
 * @param behaviorType 行为类型
 * @param rebateType   返利类型
 * @param rebateConfig 返利配置值
 * @param rebateDesc   返利说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
public record BehaviorRebateConfigEntity(
        BehaviorTypeVO behaviorType,
        RebateTypeVO rebateType,
        String rebateConfig,
        String rebateDesc
) {
}
