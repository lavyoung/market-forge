package com.lavyoung.marketforge.domain.activity.service.rule;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;

/**
 *
 * 下单规则过滤接口
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public interface IActionChain extends IActionChainArmory {

    boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount);
}
