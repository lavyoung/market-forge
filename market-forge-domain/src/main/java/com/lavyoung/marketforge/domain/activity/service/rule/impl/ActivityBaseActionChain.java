package com.lavyoung.marketforge.domain.activity.service.rule.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.service.rule.AbstractActionChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * 活动商品基础信息处理
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain extends AbstractActionChain {

    @Override
    public boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount) {
        log.info("活动责任链-商品基础信息处理【有效期&状态】开始-sku={}, activityId={}, activityCountId={}", activitySku.sku(),
                activity.activityId(), activityCount.activityCountId());
        return next().action(activitySku, activity, activityCount);
    }
}
