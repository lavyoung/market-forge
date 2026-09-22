package com.lavyoung.marketforge.domain.activity.service.quota.rule;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;

/**
 * 活动额度下单规则过滤接口。
 * <p>
 * 每个实现负责一个活动规则校验节点，并通过责任链继续传递后续校验。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public interface IActionChain extends IActionChainArmory {

    /**
     * 执行活动额度下单规则校验。
     *
     * @param activitySku   活动 SKU 实体
     * @param activity      活动实体
     * @param activityCount 活动次数配置实体
     * @return 校验通过返回 true
     */
    boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount);
}
